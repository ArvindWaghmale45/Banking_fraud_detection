package com.fraud.backend.service;

import com.fraud.backend.entity.Dataset;
import com.fraud.backend.entity.User;
import com.fraud.backend.exception.InvalidDatasetException;
import com.fraud.backend.repository.DatasetRepository;
import com.fraud.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatasetServiceTests {

    private static final String HEADER = "Time,"
            + java.util.stream.IntStream.rangeClosed(1, 28)
                    .mapToObj(index -> "V" + index)
                    .collect(java.util.stream.Collectors.joining(","))
            + ",Amount,Class";

    @TempDir
    Path uploadDirectory;

    @Test
    void uploadStoresCsvAndPersistsOwnedMetadata() throws IOException {
        DatasetRepository datasetRepository = mock(DatasetRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        DatasetStorageService storageService = new DatasetStorageService(uploadDirectory.toString());
        User user = new User("Alex Smith", "alex@example.com", "$2a$hash", User.Role.ANALYST);
        when(userRepository.findByEmailIgnoreCase("alex@example.com")).thenReturn(Optional.of(user));
        when(datasetRepository.save(any(Dataset.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DatasetService service = new DatasetService(datasetRepository, userRepository, storageService, 1024 * 1024);
        String row = java.util.stream.IntStream.range(0, 31)
                .mapToObj(index -> "0")
                .collect(java.util.stream.Collectors.joining(","));
        MockMultipartFile file = csvFile("transactions.csv", HEADER + "\n" + row + "\n" + row + "\n");

        var response = service.upload("alex@example.com", file);

        assertEquals("transactions.csv", response.fileName());
        assertEquals(2L, response.totalRecords());
        assertEquals(0L, response.fraudRecords());
        assertEquals(Dataset.Status.UPLOADED, response.status());
        try (var storedFiles = Files.list(uploadDirectory)) {
            assertEquals(1, storedFiles.count());
        }
        verify(datasetRepository).save(any(Dataset.class));
    }

    @Test
    void uploadRejectsNonCsvAndDoesNotPersistMetadata() {
        DatasetRepository datasetRepository = mock(DatasetRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        DatasetService service = new DatasetService(
                datasetRepository,
                userRepository,
                new DatasetStorageService(uploadDirectory.toString()),
                1024 * 1024
        );

        assertThrows(
                InvalidDatasetException.class,
                () -> service.upload("alex@example.com", csvFile("transactions.txt", HEADER + "\n"))
        );

        verify(userRepository, never()).findByEmailIgnoreCase(any());
        verify(datasetRepository, never()).save(any(Dataset.class));
    }

    @Test
    void uploadRejectsFilesExceedingConfiguredLimit() {
        DatasetRepository datasetRepository = mock(DatasetRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        DatasetService service = new DatasetService(
                datasetRepository,
                userRepository,
                new DatasetStorageService(uploadDirectory.toString()),
                10
        );

        assertThrows(
                InvalidDatasetException.class,
                () -> service.upload("alex@example.com", csvFile("transactions.csv", HEADER + "\n"))
        );

        verify(datasetRepository, never()).save(any(Dataset.class));
    }

    private MockMultipartFile csvFile(String fileName, String contents) {
        return new MockMultipartFile("file", fileName, "text/csv", contents.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
