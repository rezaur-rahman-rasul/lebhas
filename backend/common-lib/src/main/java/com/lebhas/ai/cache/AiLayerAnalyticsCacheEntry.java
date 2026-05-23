package com.lebhas.ai.cache;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AiLayerAnalyticsCacheEntry(
        UUID layerAnalyticsId,
        UUID layerId,
        UUID providerId,
        String modelName,
        long totalExecutions,
        long successfulExecutions,
        long failedExecutions,
        BigDecimal avgExecutionTimeMs,
        BigDecimal avgExecutionCostUsd,
        BigDecimal avgQualityScore,
        Instant cachedAt
) {
    public AiLayerAnalyticsCacheEntry {
        modelName = normalize(modelName);
        avgExecutionTimeMs = defaultZero(avgExecutionTimeMs);
        avgExecutionCostUsd = defaultZero(avgExecutionCostUsd);
        avgQualityScore = defaultZero(avgQualityScore);
        cachedAt = cachedAt == null ? Instant.now() : cachedAt;
    }

    private static BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
