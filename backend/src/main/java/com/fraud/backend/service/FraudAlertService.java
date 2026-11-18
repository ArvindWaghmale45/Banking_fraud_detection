package com.fraud.backend.service;

import com.fraud.backend.entity.FraudAlert;
import com.fraud.backend.repository.FraudAlertRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class FraudAlertService {

    private final FraudAlertRepository fraudAlertRepository;

    public FraudAlertService(FraudAlertRepository fraudAlertRepository) {
        this.fraudAlertRepository = fraudAlertRepository;
    }

    @Transactional(readOnly = true)
    public Page<FraudAlert> getAlerts(FraudAlert.Status status, Pageable pageable) {
        if (status == null) {
            return fraudAlertRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return fraudAlertRepository.findByStatusOrderByCreatedAtDesc(
                status, pageable
        );
    }

    @Transactional(readOnly = true)
    public FraudAlert getAlertById(Long id) {
        return fraudAlertRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Fraud alert not found: " + id)
                );
    }

    @Transactional
    public FraudAlert updateAlert(
            Long id,
            FraudAlert.Status status,
            String reviewNotes
    ) {
        if (status == null) {
            throw new IllegalArgumentException("Alert status is required");
        }

        FraudAlert alert = fraudAlertRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Fraud alert not found: " + id)
                );

        alert.setStatus(status);
        alert.setReviewNotes(reviewNotes);

        if (status == FraudAlert.Status.OPEN) {
            alert.setReviewedAt(null);
        } else {
            alert.setReviewedAt(LocalDateTime.now());
        }

        return fraudAlertRepository.save(alert);
    }

    public long countByStatus(FraudAlert.Status status) {
        return fraudAlertRepository.countByStatus(status);
    }

    public long countBySeverity(FraudAlert.Severity severity) {
        return fraudAlertRepository.countBySeverity(severity);
    }
}
