package com.fraud.backend.service;

import com.fraud.backend.dto.DatasetResponse;
import com.fraud.backend.entity.Dataset;
import com.fraud.backend.entity.User;
import com.fraud.backend.exception.DatasetNotFoundException;
import com.fraud.backend.exception.DatasetStorageException;
import com.fraud.backend.exception.InvalidDatasetException;
import com.fraud.backend.repository.DatasetRepository;
import com.fraud.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

@Service
public class DatasetService {

    private final DatasetRepository datasetRepository;
    private final UserRepository userRepository;
    private final DatasetStorageService storageService;
    private final long maximumFileSize;

    public DatasetService(
            DatasetRepository datasetRepository,
            UserRepository userRepository,
            DatasetStorageService storageService,
            @Value("${app.upload.max-file-size-bytes:209715200}") long maximumFileSize
    ) {
        this.datasetRepository = datasetRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.maximumFileSize = maximumFileSize;
    }

    @Transactional
    public DatasetResponse upload(String userEmail, MultipartFile file) {
        String fileName = validateFile(file);
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists."));

        long records;
        try {
            records = CsvDatasetInspector.countRecords(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)
            );
        } catch (IOException exception) {
            throw new DatasetStorageException("Could not read the uploaded CSV file.", exception);
        }

        String storageKey = storageService.store(file);
        try {
            Dataset dataset = new Dataset(fileName, file.getSize(), storageKey, user);
            dataset.setTotalRecords(records);
            dataset.setStatus(Dataset.Status.UPLOADED);
            return DatasetResponse.from(datasetRepository.save(dataset));
        } catch (DataAccessException exception) {
            storageService.delete(storageKey);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public Page<DatasetResponse> list(String userEmail, Pageable pageable) {
        return datasetRepository.findByUploadedByEmailIgnoreCaseOrderByUploadDateDesc(userEmail, pageable)
                .map(DatasetResponse::from);
    }

    @Transactional(readOnly = true)
    public DatasetResponse get(String userEmail, Long id) {
        return DatasetResponse.from(findOwnedDataset(userEmail, id));
    }

    @Transactional
    public void delete(String userEmail, Long id) {
        Dataset dataset = findOwnedDataset(userEmail, id);
        storageService.delete(dataset.getStorageKey());
        datasetRepository.delete(dataset);
    }

    private Dataset findOwnedDataset(String userEmail, Long id) {
        return datasetRepository.findByIdAndUploadedByEmailIgnoreCase(id, userEmail)
                .orElseThrow(() -> new DatasetNotFoundException(id));
    }

    private String validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidDatasetException("Choose a non-empty CSV file to upload.");
        }
        if (maximumFileSize <= 0) {
            throw new IllegalStateException("The maximum dataset upload size must be positive.");
        }
        if (file.getSize() > maximumFileSize) {
            throw new InvalidDatasetException(
                    "The CSV file exceeds the maximum size of " + maximumFileSize + " bytes."
            );
        }

        String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("");
        String fileName = originalName.replace('\\', '/');
        fileName = fileName.substring(fileName.lastIndexOf('/') + 1).trim();
        if (fileName.isBlank() || fileName.length() > 255
                || fileName.chars().anyMatch(Character::isISOControl)
                || !fileName.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new InvalidDatasetException("Upload a CSV file with a valid file name.");
        }
        return fileName;
    }
}
