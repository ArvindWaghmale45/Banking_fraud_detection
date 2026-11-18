package com.fraud.backend.repository;

import com.fraud.backend.entity.Prediction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    Page<Prediction> findByTransactionId(Long transactionId, Pageable pageable);

    Page<Prediction> findByPredictionStatus(
            Prediction.Status status,
            Pageable pageable
    );

    long countByTransactionDatasetId(Long datasetId);
}
