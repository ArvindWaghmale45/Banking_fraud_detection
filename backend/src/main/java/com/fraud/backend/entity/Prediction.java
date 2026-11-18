package com.fraud.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "predictions", indexes = {
        @Index(name = "idx_predictions_transaction", columnList = "transaction_id"),
        @Index(name = "idx_predictions_time", columnList = "prediction_time")
})
public class Prediction {

    public enum Status {
        PENDING,
        COMPLETED,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column
    private Integer predictedClass;

    @Column(precision = 7, scale = 6)
    private BigDecimal fraudProbability;

    @Column(length = 100)
    private String modelName;

    @Column(length = 50)
    private String modelVersion;

    @Column(nullable = false, updatable = false)
    private LocalDateTime predictionTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status predictionStatus = Status.PENDING;

    @OneToOne(mappedBy = "prediction")
    private FraudAlert fraudAlert;

    protected Prediction() {
    }

    public Prediction(Transaction transaction, String modelName, String modelVersion) {
        this.transaction = transaction;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
    }

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        predictionTime = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public Integer getPredictedClass() {
        return predictedClass;
    }

    public void setPredictedClass(Integer predictedClass) {
        this.predictedClass = predictedClass;
    }

    public BigDecimal getFraudProbability() {
        return fraudProbability;
    }

    public void setFraudProbability(BigDecimal fraudProbability) {
        this.fraudProbability = fraudProbability;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public LocalDateTime getPredictionTime() {
        return predictionTime;
    }

    public Status getPredictionStatus() {
        return predictionStatus;
    }

    public void setPredictionStatus(Status predictionStatus) {
        this.predictionStatus = predictionStatus;
    }

    public FraudAlert getFraudAlert() {
        return fraudAlert;
    }
}
