package com.fraud.backend.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ManualFeatures(
        @NotNull BigDecimal v1, @NotNull BigDecimal v2, @NotNull BigDecimal v3,
        @NotNull BigDecimal v4, @NotNull BigDecimal v5, @NotNull BigDecimal v6,
        @NotNull BigDecimal v7, @NotNull BigDecimal v8, @NotNull BigDecimal v9,
        @NotNull BigDecimal v10, @NotNull BigDecimal v11, @NotNull BigDecimal v12,
        @NotNull BigDecimal v13, @NotNull BigDecimal v14, @NotNull BigDecimal v15,
        @NotNull BigDecimal v16, @NotNull BigDecimal v17, @NotNull BigDecimal v18,
        @NotNull BigDecimal v19, @NotNull BigDecimal v20, @NotNull BigDecimal v21,
        @NotNull BigDecimal v22, @NotNull BigDecimal v23, @NotNull BigDecimal v24,
        @NotNull BigDecimal v25, @NotNull BigDecimal v26, @NotNull BigDecimal v27,
        @NotNull BigDecimal v28
) {}
