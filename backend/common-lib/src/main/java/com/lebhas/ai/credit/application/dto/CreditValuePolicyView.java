package com.lebhas.ai.credit.application.dto;

import com.lebhas.ai.credit.domain.FreeSignupCreditMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreditValuePolicyView(
        UUID id,
        String currency,
        BigDecimal creditUsdValue,
        BigDecimal averageProviderCostPerCreativeUsd,
        BigDecimal providerCostMultiplier,
        BigDecimal calculatedCreativeCostUsd,
        BigDecimal calculatedCreativeCreditCost,
        boolean freeSignupCreditEnabled,
        FreeSignupCreditMode freeSignupMode,
        BigDecimal freeSignupCredits,
        BigDecimal freeSignupUsdValue,
        BigDecimal freeSignupPercentage,
        BigDecimal freeSignupUsdEquivalent,
        boolean oneTimePerWorkspace,
        BigDecimal minimumWalletBalanceWarning,
        boolean active,
        Instant effectiveFrom,
        Instant updatedAt
) {
}
