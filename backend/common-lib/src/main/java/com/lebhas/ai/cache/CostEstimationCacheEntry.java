package com.lebhas.ai.cache;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CostEstimationCacheEntry(
        UUID workspaceId,
        UUID creativeRequestId,
        UUID pipelineId,
        BigDecimal estimatedCost,
        String currency,
        Map<String, Object> breakdown,
        Instant cachedAt
) {
    public CostEstimationCacheEntry {
        currency = normalize(currency);
        breakdown = breakdown == null ? Map.of() : Map.copyOf(breakdown);
        cachedAt = cachedAt == null ? Instant.now() : cachedAt;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
