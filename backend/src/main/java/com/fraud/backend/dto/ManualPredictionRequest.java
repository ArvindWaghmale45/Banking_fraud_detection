package com.fraud.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ManualPredictionRequest(
        @NotNull @DecimalMin("0") BigDecimal time,
        @NotNull @Valid ManualFeatures features,
        @NotNull @DecimalMin("0") BigDecimal amount
) {}
