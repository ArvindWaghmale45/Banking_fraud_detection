package com.fraud.backend.service;

import com.fraud.backend.entity.Dataset;
import com.fraud.backend.entity.FraudAlert;
import com.fraud.backend.entity.Prediction;
import com.fraud.backend.entity.Transaction;
import com.fraud.backend.exception.DatasetNotFoundException;
import com.fraud.backend.repository.DatasetRepository;
import com.fraud.backend.repository.FraudAlertRepository;
import com.fraud.backend.repository.PredictionRepository;
import com.fraud.backend.repository.TransactionRepository;
import jakarta.persistence.EntityManager;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class PredictionImportService {

    private static final int BATCH_SIZE = 500;
    private static final String MODEL_NAME = "Logistic Regression (class-weight balanced)";
    private static final String MODEL_VERSION = "baseline-1";
    private static final Set<String> REQUIRED_COLUMNS = buildRequiredColumns();

    private final DatasetRepository datasetRepository;
    private final TransactionRepository transactionRepository;
    private final PredictionRepository predictionRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final EntityManager entityManager;

    public PredictionImportService(
            DatasetRepository datasetRepository,
            TransactionRepository transactionRepository,
            PredictionRepository predictionRepository,
            FraudAlertRepository fraudAlertRepository,
            EntityManager entityManager
    ) {
        this.datasetRepository = datasetRepository;
        this.transactionRepository = transactionRepository;
        this.predictionRepository = predictionRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.entityManager = entityManager;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importPredictions(
            Long datasetId,
            String userEmail,
            MultipartFile file
    ) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select a prediction CSV file.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null
                || !originalFilename.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new IllegalArgumentException("Only CSV files are supported.");
        }

        datasetRepository.findByIdAndUploadedByEmailIgnoreCase(datasetId, userEmail)
                .orElseThrow(() ->
                        new DatasetNotFoundException(datasetId)
                );

        if (transactionRepository.countByDatasetId(datasetId) > 0
                || predictionRepository.countByTransactionDatasetId(datasetId) > 0) {
            throw new IllegalArgumentException(
                    "Predictions have already been imported for this dataset."
            );
        }

        long totalImported = 0;
        long fraudAlertsCreated = 0;
        long actualFraudRecords = 0;
        long actualGenuineRecords = 0;
        List<Transaction> transactionBatch = new ArrayList<>(BATCH_SIZE);
        List<Prediction> predictionBatch = new ArrayList<>(BATCH_SIZE);
        List<FraudAlert> alertBatch = new ArrayList<>();

        try (
                PushbackReader bomReader = openUtf8Reader(file);
                CSVParser parser = createParser(bomReader)
        ) {
            validateHeaders(parser.getHeaderMap().keySet());

            for (CSVRecord record : parser) {
                if (record.size() != REQUIRED_COLUMNS.size()) {
                    throw new IllegalArgumentException(
                            "Invalid CSV row at record " + record.getRecordNumber()
                    );
                }

                int actualClass = parseClass(record.get("Actual_Class"), "Actual_Class");
                int predictedClass = parseClass(record.get("Predicted_Class"), "Predicted_Class");
                validatePredictionStatus(record.get("Prediction_Status"), predictedClass,
                        record.getRecordNumber());

                BigDecimal amount = parseDecimal(record.get("Amount"), "Amount");
                BigDecimal probability = parseDecimal(
                        record.get("Fraud_Probability"),
                        "Fraud_Probability"
                );
                if (probability.compareTo(BigDecimal.ZERO) < 0
                        || probability.compareTo(BigDecimal.ONE) > 0) {
                    throw new IllegalArgumentException(
                            "Fraud probability must be between 0 and 1 at record "
                                    + record.getRecordNumber()
                    );
                }

                Map<String, BigDecimal> features = new HashMap<>();
                for (int i = 1; i <= 28; i++) {
                    String featureName = "V" + i;
                    features.put(featureName, parseDecimal(record.get(featureName), featureName));
                }

                Transaction transaction = new Transaction(datasetReference(datasetId), null,
                        amount, actualClass);
                transaction.setFeatures(features);

                Prediction prediction = new Prediction(transaction, MODEL_NAME, MODEL_VERSION);
                prediction.setPredictedClass(predictedClass);
                prediction.setFraudProbability(probability.setScale(6, RoundingMode.HALF_UP));
                prediction.setPredictionStatus(Prediction.Status.COMPLETED);

                transactionBatch.add(transaction);
                predictionBatch.add(prediction);

                if (actualClass == 1) {
                    actualFraudRecords++;
                } else {
                    actualGenuineRecords++;
                }

                if (predictedClass == 1) {
                    alertBatch.add(new FraudAlert(
                            transaction,
                            prediction,
                            FraudAlert.Severity.HIGH
                    ));
                    fraudAlertsCreated++;
                }

                if (transactionBatch.size() >= BATCH_SIZE) {
                    saveBatch(datasetId, transactionBatch, predictionBatch, alertBatch);
                    totalImported += transactionBatch.size();
                    transactionBatch.clear();
                    predictionBatch.clear();
                    alertBatch.clear();
                }
            }

            if (!transactionBatch.isEmpty()) {
                saveBatch(datasetId, transactionBatch, predictionBatch, alertBatch);
                totalImported += transactionBatch.size();
            }
        }

        if (totalImported == 0) {
            throw new IllegalArgumentException("The prediction CSV contains no records.");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("datasetId", datasetId);
        result.put("modelName", MODEL_NAME);
        result.put("modelVersion", MODEL_VERSION);
        result.put("totalImported", totalImported);
        result.put("actualFraudRecords", actualFraudRecords);
        result.put("actualGenuineRecords", actualGenuineRecords);
        result.put("fraudAlertsCreated", fraudAlertsCreated);
        result.put("message", "Prediction records imported successfully.");
        return result;
    }

    private CSVParser createParser(PushbackReader reader) throws IOException {
        return CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .get()
                .parse(reader);
    }

    private static PushbackReader openUtf8Reader(MultipartFile file) throws IOException {
        PushbackReader reader = new PushbackReader(
                new BufferedReader(new InputStreamReader(
                        file.getInputStream(),
                        StandardCharsets.UTF_8
                )),
                1
        );
        int firstCharacter = reader.read();
        if (firstCharacter != '\uFEFF' && firstCharacter != -1) {
            reader.unread(firstCharacter);
        }
        return reader;
    }

    private Dataset datasetReference(Long datasetId) {
        return entityManager.getReference(Dataset.class, datasetId);
    }

    private void saveBatch(
            Long datasetId,
            List<Transaction> transactions,
            List<Prediction> predictions,
            List<FraudAlert> alerts
    ) {
        Dataset managedDataset = entityManager.getReference(Dataset.class, datasetId);
        for (Transaction transaction : transactions) {
            transaction.setDataset(managedDataset);
        }

        transactionRepository.saveAll(transactions);
        transactionRepository.flush();
        predictionRepository.saveAll(predictions);
        predictionRepository.flush();

        if (!alerts.isEmpty()) {
            fraudAlertRepository.saveAll(alerts);
            fraudAlertRepository.flush();
        }

        entityManager.clear();
    }

    private static int parseClass(String value, String column) {
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed != 0 && parsed != 1) {
                throw new IllegalArgumentException(column + " must contain only 0 or 1.");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid " + column + " value: " + value);
        }
    }

    private static BigDecimal parseDecimal(String value, String column) {
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Invalid numeric value in " + column + ": " + value
            );
        }
    }

    private static void validatePredictionStatus(
            String status,
            int predictedClass,
            long recordNumber
    ) {
        String expectedStatus = predictedClass == 1
                ? "Suspicious"
                : "Genuine";
        if (!expectedStatus.equalsIgnoreCase(status.trim())) {
            throw new IllegalArgumentException(
                    "Prediction_Status does not match Predicted_Class at record " + recordNumber
            );
        }
    }

    private static void validateHeaders(Set<String> headers) {
        if (!headers.equals(REQUIRED_COLUMNS)) {
            throw new IllegalArgumentException(
                    "Invalid prediction CSV headers. Expected Time, V1-V28, Amount, "
                            + "Actual_Class, Fraud_Probability, Predicted_Class and Prediction_Status."
            );
        }
    }

    private static Set<String> buildRequiredColumns() {
        Set<String> columns = new HashSet<>();
        columns.add("Time");
        columns.add("Amount");
        columns.add("Actual_Class");
        columns.add("Fraud_Probability");
        columns.add("Predicted_Class");
        columns.add("Prediction_Status");
        for (int i = 1; i <= 28; i++) {
            columns.add("V" + i);
        }
        return Set.copyOf(columns);
    }
}
