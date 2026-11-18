package com.fraud.backend.service;

import com.fraud.backend.dto.ManualFeatures;
import com.fraud.backend.dto.ManualPredictionRequest;
import com.fraud.backend.dto.ManualPredictionResponse;
import com.fraud.backend.entity.Dataset;
import com.fraud.backend.entity.FraudAlert;
import com.fraud.backend.entity.Prediction;
import com.fraud.backend.entity.Transaction;
import com.fraud.backend.entity.User;
import com.fraud.backend.repository.DatasetRepository;
import com.fraud.backend.repository.FraudAlertRepository;
import com.fraud.backend.repository.PredictionRepository;
import com.fraud.backend.repository.TransactionRepository;
import com.fraud.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ManualPredictionService {
    private final DatasetRepository datasetRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final PredictionRepository predictionRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final String mlUrl;

    public ManualPredictionService(
            DatasetRepository datasetRepository,
            UserRepository userRepository,
            TransactionRepository transactionRepository,
            PredictionRepository predictionRepository,
            FraudAlertRepository fraudAlertRepository,
            @Value("${app.ml.url:http://localhost:8000}") String mlUrl
    ) {
        this.datasetRepository = datasetRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.predictionRepository = predictionRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.mlUrl = mlUrl;
    }

    @Transactional
    public ManualPredictionResponse predict(String userEmail, ManualPredictionRequest request) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists."));
        String result = callMl(request);

        Dataset dataset = datasetRepository
                .findFirstByUploadedByEmailIgnoreCaseAndFileNameOrderByIdDesc(userEmail, "manual-predictions.csv")
                .orElseGet(() -> {
                    Dataset manual = new Dataset(
                            "manual-predictions.csv",
                            0L,
                            UUID.randomUUID().toString(),
                            user
                    );
                    manual.setStatus(Dataset.Status.READY);
                    return datasetRepository.save(manual);
                });

        Map<String, BigDecimal> features = features(request);
        Transaction transaction = new Transaction(
                dataset,
                LocalDateTime.now(),
                request.amount(),
                null
        );
        transaction.setFeatures(features);
        transactionRepository.save(transaction);

        BigDecimal probability = new BigDecimal(jsonNumber(result, "fraud_probability"))
                .setScale(6, RoundingMode.HALF_UP);
        BigDecimal threshold = new BigDecimal(jsonNumber(result, "threshold"))
                .setScale(6, RoundingMode.HALF_UP);
        String predictionName = jsonString(result, "prediction");
        String riskLevel = jsonString(result, "risk_level");
        int predictedClass = "Suspicious".equalsIgnoreCase(predictionName) ? 1 : 0;

        Prediction prediction = new Prediction(transaction, "Random Forest", "rf-1");
        prediction.setPredictedClass(predictedClass);
        prediction.setFraudProbability(probability);
        prediction.setPredictionStatus(Prediction.Status.COMPLETED);
        predictionRepository.save(prediction);

        Long alertId = null;
        if (predictedClass == 1) {
            FraudAlert.Severity severity = switch (riskLevel) {
                case "HIGH" -> FraudAlert.Severity.CRITICAL;
                case "MEDIUM" -> FraudAlert.Severity.HIGH;
                default -> FraudAlert.Severity.MEDIUM;
            };
            FraudAlert alert = new FraudAlert(transaction, prediction, severity);
            fraudAlertRepository.save(alert);
            alertId = alert.getId();
        }

        dataset.setTotalRecords(dataset.getTotalRecords() + 1);
        if (predictedClass == 1) {
            dataset.setFraudRecords(dataset.getFraudRecords() + 1);
        } else {
            dataset.setGenuineRecords(dataset.getGenuineRecords() + 1);
        }
        datasetRepository.save(dataset);

        return new ManualPredictionResponse(
                transaction.getId(),
                prediction.getId(),
                alertId,
                predictionName,
                riskLevel,
                probability,
                threshold
        );
    }

    private String callMl(ManualPredictionRequest request) {
        try {
            ManualFeatures f = request.features();
            String payload = """
                    {"time":%s,"amount":%s,"v1":%s,"v2":%s,"v3":%s,"v4":%s,"v5":%s,
                    "v6":%s,"v7":%s,"v8":%s,"v9":%s,"v10":%s,"v11":%s,"v12":%s,
                    "v13":%s,"v14":%s,"v15":%s,"v16":%s,"v17":%s,"v18":%s,"v19":%s,
                    "v20":%s,"v21":%s,"v22":%s,"v23":%s,"v24":%s,"v25":%s,"v26":%s,
                    "v27":%s,"v28":%s}
                    """.formatted(
                    request.time(), request.amount(),
                    f.v1(), f.v2(), f.v3(), f.v4(), f.v5(), f.v6(), f.v7(), f.v8(), f.v9(),
                    f.v10(), f.v11(), f.v12(), f.v13(), f.v14(), f.v15(), f.v16(), f.v17(), f.v18(),
                    f.v19(), f.v20(), f.v21(), f.v22(), f.v23(), f.v24(), f.v25(), f.v26(), f.v27(), f.v28()
            );

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(mlUrl + "/predict"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() != 200) {
                throw new IllegalStateException("ML service returned HTTP " + response.statusCode());
            }
            return response.body();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not score transaction with the ML service.", exception);
        }
    }

    private String jsonNumber(String json, String key) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\"" + key + "\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?)")
                .matcher(json);
        if (!matcher.find()) {
            throw new IllegalStateException("ML response omitted " + key);
        }
        return matcher.group(1);
    }

    private String jsonString(String json, String key) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"")
                .matcher(json);
        if (!matcher.find()) {
            throw new IllegalStateException("ML response omitted " + key);
        }
        return matcher.group(1);
    }

    private Map<String, BigDecimal> features(ManualPredictionRequest request) {
        ManualFeatures f = request.features();
        Map<String, BigDecimal> values = new HashMap<>();
        values.put("V1", f.v1()); values.put("V2", f.v2()); values.put("V3", f.v3());
        values.put("V4", f.v4()); values.put("V5", f.v5()); values.put("V6", f.v6());
        values.put("V7", f.v7()); values.put("V8", f.v8()); values.put("V9", f.v9());
        values.put("V10", f.v10()); values.put("V11", f.v11()); values.put("V12", f.v12());
        values.put("V13", f.v13()); values.put("V14", f.v14()); values.put("V15", f.v15());
        values.put("V16", f.v16()); values.put("V17", f.v17()); values.put("V18", f.v18());
        values.put("V19", f.v19()); values.put("V20", f.v20()); values.put("V21", f.v21());
        values.put("V22", f.v22()); values.put("V23", f.v23()); values.put("V24", f.v24());
        values.put("V25", f.v25()); values.put("V26", f.v26()); values.put("V27", f.v27());
        values.put("V28", f.v28());
        values.put("Time", request.time());
        values.put("Amount", request.amount());
        return values;
    }
}
