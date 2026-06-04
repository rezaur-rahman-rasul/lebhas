package com.lebhas.ai.credit.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProviderCreditPoolCommand(
        String currency,
        @NotNull @DecimalMin(value = "0.0") BigDecimal providerBalanceAmount,
        @NotNull @DecimalMin(value = "0.0") BigDecimal internalCreditEquivalent,
        @NotNull @DecimalMin(value = "0.0") BigDecimal lowBalanceThreshold
) {
}
