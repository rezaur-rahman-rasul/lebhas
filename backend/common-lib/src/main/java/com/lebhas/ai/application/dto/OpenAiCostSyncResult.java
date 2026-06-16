package com.lebhas.ai.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OpenAiCostSyncResult(
        UUID providerId,
        String providerCode,
        boolean success,
        String message,
        Integer httpStatus,
        BigDecimal previousSpendUsd,
        BigDecimal totalCostSpentUsd,
        BigDecimal previousBalanceUsd,
        BigDecimal estimatedRemainingBalanceUsd,
        BigDecimal internalCredits,
        Instant syncedAt
) {
}
