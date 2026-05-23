package com.lebhas.ai.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AiLayerAnalyticsView(
        UUID id,
        UUID layerId,
        UUID providerId,
        String modelName,
        long totalExecutions,
        long successfulExecutions,
        long failedExecutions,
        BigDecimal avgExecutionTimeMs,
        BigDecimal avgExecutionCostUsd,
        BigDecimal avgQualityScore,
        Instant createdAt,
        Instant updatedAt
) {
}
