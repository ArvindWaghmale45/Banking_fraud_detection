package com.fraud.backend.repository;

import com.fraud.backend.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByDatasetId(Long datasetId, Pageable pageable);

    Page<Transaction> findByDatasetIdAndActualClass(
            Long datasetId,
            Integer actualClass,
            Pageable pageable
    );

    long countByDatasetId(Long datasetId);
}
