package com.lebhas.ai.credit.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProviderCreditExchangePolicyView(
        UUID id,
        UUID providerId,
        BigDecimal internalCreditPerProviderUnit,
        BigDecimal freeSignupCreditPercentage,
        boolean freeSignupCreditEnabled,
        BigDecimal maxFreeSignupCredits,
        BigDecimal minProviderBalanceRequired,
        BigDecimal fallbackFreeCredits,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
