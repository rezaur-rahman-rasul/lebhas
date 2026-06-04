package com.lebhas.ai.credit.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProviderCreditExchangePolicyCommand(
        @NotNull @DecimalMin(value = "0.0001") BigDecimal internalCreditPerProviderUnit,
        @NotNull @DecimalMin(value = "0.0") BigDecimal freeSignupCreditPercentage,
        boolean freeSignupCreditEnabled,
        @NotNull @DecimalMin(value = "0.0") BigDecimal maxFreeSignupCredits,
        @NotNull @DecimalMin(value = "0.0") BigDecimal minProviderBalanceRequired,
        @NotNull @DecimalMin(value = "0.0") BigDecimal fallbackFreeCredits,
        boolean active
) {
}
