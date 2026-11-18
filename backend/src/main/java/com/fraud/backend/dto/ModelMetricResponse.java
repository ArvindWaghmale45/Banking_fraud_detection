package com.fraud.backend.dto;

import com.fraud.backend.entity.ModelMetric;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ModelMetricResponse(
        Long id,
        String modelName,
        String modelVersion,
        BigDecimal accuracy,
        BigDecimal precision,
        BigDecimal recall,
        BigDecimal f1Score,
        BigDecimal rocAuc,
        BigDecimal prAuc,
        LocalDateTime trainingDate
) {
    public static ModelMetricResponse from(ModelMetric metric) {
        return new ModelMetricResponse(
                metric.getId(),
                metric.getModelName(),
                metric.getModelVersion(),
                metric.getAccuracy(),
                metric.getPrecision(),
                metric.getRecall(),
                metric.getF1Score(),
                metric.getRocAuc(),
                metric.getPrAuc(),
                metric.getTrainingDate()
        );
    }
}
