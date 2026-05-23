package com.lebhas.creativesaas.creative.interfaces;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

public record ConfigureLayerCostPolicyRequest(
        @NotBlank @Size(max = 120) String policyCode,
        boolean enabled,
        @Min(1) int priorityOrder,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{3}$") String currency,
        @DecimalMin(value = "0.0") BigDecimal maxCostPerRun,
        @NotNull Map<String, Object> costRules,
        @NotNull Map<String, Object> budgetMetadata
) {
}
