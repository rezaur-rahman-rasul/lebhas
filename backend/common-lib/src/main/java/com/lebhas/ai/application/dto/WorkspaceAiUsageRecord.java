package com.lebhas.ai.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WorkspaceAiUsageRecord(
        UUID workspaceId,
        long generationRequests,
        long generatedVersions,
        BigDecimal creditsConsumed,
        BigDecimal estimatedCostUsd,
        long failures,
        BigDecimal generationTimeMs
) {
}
