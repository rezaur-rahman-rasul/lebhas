package com.lebhas.ai.credit.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProviderCreditPoolView(
        UUID id,
        UUID providerId,
        String currency,
        BigDecimal providerBalanceAmount,
        BigDecimal internalCreditEquivalent,
        BigDecimal reservedInternalCredits,
        BigDecimal usedInternalCredits,
        BigDecimal availableInternalCredits,
        BigDecimal lowBalanceThreshold,
        boolean lowBalance,
        Instant createdAt,
        Instant updatedAt
) {
}
