package com.lebhas.ai.credit.application.dto;

import com.lebhas.ai.credit.domain.FreeSignupCreditMode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record CreditValuePolicyCommand(
        String currency,
        @NotNull @DecimalMin(value = "0.000001") BigDecimal creditUsdValue,
        @NotNull @DecimalMin(value = "0.0") BigDecimal averageProviderCostPerCreativeUsd,
        @NotNull @DecimalMin(value = "1.0") BigDecimal providerCostMultiplier,
        boolean freeSignupCreditEnabled,
        @NotNull FreeSignupCreditMode freeSignupMode,
        @NotNull @DecimalMin(value = "0.0") BigDecimal freeSignupCredits,
        @NotNull @DecimalMin(value = "0.0") BigDecimal freeSignupUsdValue,
        @NotNull @DecimalMin(value = "0.0") @DecimalMax(value = "100.0") BigDecimal freeSignupPercentage,
        boolean oneTimePerWorkspace,
        @NotNull @DecimalMin(value = "0.0") BigDecimal minimumWalletBalanceWarning,
        boolean active,
        Instant effectiveFrom
) {
}
