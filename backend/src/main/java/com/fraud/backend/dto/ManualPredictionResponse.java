package com.fraud.backend.dto;

import java.math.BigDecimal;

public record ManualPredictionResponse(
        Long transactionId,
        Long predictionId,
        Long alertId,
        String prediction,
        String riskLevel,
        BigDecimal fraudProbability,
        BigDecimal threshold
) {}
