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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "datasets", indexes = {
        @Index(name = "idx_datasets_upload_date", columnList = "upload_date"),
        @Index(name = "idx_datasets_status", columnList = "status")
})
public class Dataset {

    public enum Status {
        UPLOADED,
        PROCESSING,
        READY,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false)
    private Long fileSize;

    @Column(name = "storage_key", nullable = false, unique = true, length = 36)
    private String storageKey;

    @Column(nullable = false)
    private Long totalRecords = 0L;

    @Column(nullable = false)
    private Long fraudRecords = 0L;

    @Column(nullable = false)
    private Long genuineRecords = 0L;

    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.UPLOADED;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private User uploadedBy;

    @OneToMany(mappedBy = "dataset")
    private List<Transaction> transactions = new ArrayList<>();

    protected Dataset() {
    }

    public Dataset(String fileName, Long fileSize, String storageKey, User uploadedBy) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.storageKey = storageKey;
        this.uploadedBy = uploadedBy;
    }

    @PrePersist
    protected void onCreate() {
        uploadDate = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Long getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(Long totalRecords) {
        this.totalRecords = totalRecords;
    }

    public Long getFraudRecords() {
        return fraudRecords;
    }

    public void setFraudRecords(Long fraudRecords) {
        this.fraudRecords = fraudRecords;
    }

    public Long getGenuineRecords() {
        return genuineRecords;
    }

    public void setGenuineRecords(Long genuineRecords) {
        this.genuineRecords = genuineRecords;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public User getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(User uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }
}
