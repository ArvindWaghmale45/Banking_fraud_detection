package com.fraud.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transactions_dataset", columnList = "dataset_id"),
        @Index(name = "idx_transactions_time", columnList = "transaction_time")
})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dataset_id", nullable = false)
    private Dataset dataset;

    private LocalDateTime transactionTime;

    @Column(precision = 19, scale = 4)
    private BigDecimal amount;

    @Column
    private Integer actualClass;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, BigDecimal> features = new HashMap<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "transaction")
    private List<Prediction> predictions = new ArrayList<>();

    @OneToMany(mappedBy = "transaction")
    private List<FraudAlert> fraudAlerts = new ArrayList<>();

    protected Transaction() {
    }

    public Transaction(Dataset dataset, LocalDateTime transactionTime, BigDecimal amount, Integer actualClass) {
        this.dataset = dataset;
        this.transactionTime = transactionTime;
        this.amount = amount;
        this.actualClass = actualClass;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public LocalDateTime getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(LocalDateTime transactionTime) {
        this.transactionTime = transactionTime;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Integer getActualClass() {
        return actualClass;
    }

    public void setActualClass(Integer actualClass) {
        this.actualClass = actualClass;
    }

    public Map<String, BigDecimal> getFeatures() {
        return features;
    }

    public void setFeatures(Map<String, BigDecimal> features) {
        this.features = features;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<Prediction> getPredictions() {
        return predictions;
    }

    public List<FraudAlert> getFraudAlerts() {
        return fraudAlerts;
    }
}
