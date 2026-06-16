package com.lebhas.ai.credit.application.dto;

import com.lebhas.ai.credit.domain.FreeSignupCreditMode;

import java.math.BigDecimal;

public record CreditValuePolicyPreviewView(
        String currency,
        BigDecimal providerCostUsd,
        BigDecimal multiplier,
        BigDecimal creativeCostUsd,
        BigDecimal creditUsdValue,
        BigDecimal creativeCreditCost,
        FreeSignupCreditMode freeSignupMode,
        BigDecimal freeSignupCredits,
        BigDecimal freeSignupUsdEquivalent
) {
}
