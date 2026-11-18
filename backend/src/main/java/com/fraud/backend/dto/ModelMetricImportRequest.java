package com.fraud.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class ModelMetricImportRequest {

    @JsonProperty("model_name")
    @NotBlank
    @Size(max = 100)
    private String modelName;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @Digits(integer = 1, fraction = 18)
    private BigDecimal accuracy;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @Digits(integer = 1, fraction = 18)
    private BigDecimal precision;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @Digits(integer = 1, fraction = 18)
    private BigDecimal recall;

    @JsonProperty("f1_score")
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @Digits(integer = 1, fraction = 18)
    private BigDecimal f1Score;

    @JsonProperty("roc_auc")
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @Digits(integer = 1, fraction = 18)
    private BigDecimal rocAuc;

    @JsonProperty("pr_auc")
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @Digits(integer = 1, fraction = 18)
    private BigDecimal prAuc;

    public ModelMetricImportRequest() {
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
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
}
