package com.fraud.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "model_metrics", indexes = {
        @Index(name = "idx_model_metrics_training_date", columnList = "training_date")
})
public class ModelMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String modelName;

    @Column(nullable = false, length = 50)
    private String modelVersion;

    @Column(precision = 19, scale = 18)
    private BigDecimal accuracy;

    @Column(name = "precision_score", precision = 19, scale = 18)
    private BigDecimal precision;

    @Column(precision = 19, scale = 18)
    private BigDecimal recall;

    @Column(precision = 19, scale = 18)
    private BigDecimal f1Score;

    @Column(precision = 19, scale = 18)
    private BigDecimal rocAuc;

    @Column(precision = 19, scale = 18)
    private BigDecimal prAuc;

    @Column(nullable = false)
    private LocalDateTime trainingDate;

    protected ModelMetric() {
    }

    public ModelMetric(String modelName, String modelVersion, LocalDateTime trainingDate) {
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.trainingDate = trainingDate;
    }

    public Long getId() {
        return id;
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

    public BigDecimal getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(BigDecimal accuracy) {
        this.accuracy = accuracy;
    }

    public BigDecimal getPrecision() {
        return precision;
    }

    public void setPrecision(BigDecimal precision) {
        this.precision = precision;
    }

    public BigDecimal getRecall() {
        return recall;
    }

    public void setRecall(BigDecimal recall) {
        this.recall = recall;
    }

    public BigDecimal getF1Score() {
        return f1Score;
    }

    public void setF1Score(BigDecimal f1Score) {
        this.f1Score = f1Score;
    }

    public BigDecimal getRocAuc() {
        return rocAuc;
    }

    public void setRocAuc(BigDecimal rocAuc) {
        this.rocAuc = rocAuc;
    }

    public BigDecimal getPrAuc() {
        return prAuc;
    }

    public void setPrAuc(BigDecimal prAuc) {
        this.prAuc = prAuc;
    }

    public LocalDateTime getTrainingDate() {
        return trainingDate;
    }
}
