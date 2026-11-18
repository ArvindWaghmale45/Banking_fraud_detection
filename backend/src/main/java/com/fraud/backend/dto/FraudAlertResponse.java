package com.fraud.backend.dto;

import com.fraud.backend.entity.FraudAlert;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FraudAlertResponse {

    private Long id;
    private Long transactionId;
    private Long predictionId;
    private FraudAlert.Severity severity;
    private FraudAlert.Status status;
    private String reviewNotes;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private BigDecimal transactionAmount;

    public FraudAlertResponse(FraudAlert alert) {
        this.id = alert.getId();
        this.transactionId = alert.getTransaction().getId();
        this.predictionId = alert.getPrediction().getId();
        this.severity = alert.getSeverity();
        this.status = alert.getStatus();
        this.reviewNotes = alert.getReviewNotes();
        this.createdAt = alert.getCreatedAt();
        this.reviewedAt = alert.getReviewedAt();
        this.transactionAmount = alert.getTransaction().getAmount();
    }

    public Long getId() {
        return id;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public Long getPredictionId() {
        return predictionId;
    }

    public FraudAlert.Severity getSeverity() {
        return severity;
    }

    public FraudAlert.Status getStatus() {
        return status;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public BigDecimal getTransactionAmount() {
        return transactionAmount;
    }
}
