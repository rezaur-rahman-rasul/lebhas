package com.lebhas.creativesaas.creative.interfaces;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

public record ConfigureLayerQualityPolicyRequest(
        @NotBlank @Size(max = 120) String policyCode,
        boolean enabled,
        @Min(1) int priorityOrder,
        @DecimalMin(value = "0.0") @DecimalMax(value = "1.0") BigDecimal minQualityScore,
        @NotNull Map<String, Object> qualityRules,
        @NotNull Map<String, Object> evaluationMetadata
) {
}
