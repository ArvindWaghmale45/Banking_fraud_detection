package com.fraud.backend.service;

import com.fraud.backend.exception.DatasetStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class DatasetStorageService {

    private final Path uploadDirectory;

    public DatasetStorageService(@Value("${app.upload.directory:${user.home}/.banking-fraud-detection/uploads}") String directory) {
        this.uploadDirectory = Path.of(directory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        String storageKey = UUID.randomUUID().toString();
        Path destination = resolve(storageKey);

        try {
            Files.createDirectories(uploadDirectory);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, destination);
            }
            return storageKey;
        } catch (IOException exception) {
            throw new DatasetStorageException("Could not store the uploaded dataset.", exception);
        }
    }

    public InputStream open(String storageKey) {
        try {
            return Files.newInputStream(resolve(storageKey));
        } catch (IOException exception) {
            throw new DatasetStorageException("Could not read the stored dataset.", exception);
        }
    }

    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException exception) {
            throw new DatasetStorageException("Could not delete the stored dataset file.", exception);
        }
    }

    private Path resolve(String storageKey) {
        Path file = uploadDirectory.resolve(storageKey + ".csv").normalize();
        if (!file.getParent().equals(uploadDirectory)) {
            throw new IllegalArgumentException("Invalid dataset storage key.");
        }
        return file;
    }
}
