package com.fraud.backend.dto;

import com.fraud.backend.entity.Dataset;

import java.time.LocalDateTime;

public record DatasetResponse(
        Long id,
        String fileName,
        Long fileSize,
        Long totalRecords,
        Long fraudRecords,
        Long genuineRecords,
        LocalDateTime uploadDate,
        Dataset.Status status
) {
    public static DatasetResponse from(Dataset dataset) {
        return new DatasetResponse(
                dataset.getId(),
                dataset.getFileName(),
                dataset.getFileSize(),
                dataset.getTotalRecords(),
                dataset.getFraudRecords(),
                dataset.getGenuineRecords(),
                dataset.getUploadDate(),
                dataset.getStatus()
        );
    }
}
