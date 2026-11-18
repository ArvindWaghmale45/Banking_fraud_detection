
package com.fraud.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.fraud.backend.entity.FraudAlert;

public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {

    Page<FraudAlert> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<FraudAlert> findByStatusOrderByCreatedAtDesc(
            FraudAlert.Status status,
            Pageable pageable
    );

    long countByStatus(FraudAlert.Status status);

    long countBySeverity(FraudAlert.Severity severity);
}
