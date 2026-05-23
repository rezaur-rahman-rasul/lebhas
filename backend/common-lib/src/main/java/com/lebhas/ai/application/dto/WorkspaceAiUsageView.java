package com.lebhas.ai.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WorkspaceAiUsageView(
        UUID id,
        UUID workspaceId,
        long totalGenerationRequests,
        long totalGeneratedVersions,
        BigDecimal totalCreditsConsumed,
        BigDecimal totalEstimatedCostUsd,
        long totalFailures,
        BigDecimal avgGenerationTimeMs,
        Instant createdAt,
        Instant updatedAt
) {
}
