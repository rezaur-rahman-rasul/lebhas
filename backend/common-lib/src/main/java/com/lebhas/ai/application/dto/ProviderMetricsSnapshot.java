package com.lebhas.ai.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProviderMetricsSnapshot(
        UUID metricsId,
        UUID providerId,
        String providerCode,
        String providerName,
        String modelName,
        long totalRequests,
        long successfulRequests,
        long failedRequests,
        BigDecimal avgLatencyMs,
        BigDecimal avgCostUsd,
        BigDecimal avgQualityScore,
        BigDecimal uptimePercentage,
        Instant lastFailureAt,
        Instant lastSuccessAt
) {
}
