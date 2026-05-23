package com.lebhas.ai.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProviderHealthSnapshot(
        UUID providerId,
        String providerCode,
        String providerName,
        String healthStatus,
        BigDecimal reliabilityScore,
        BigDecimal uptimePercentage,
        long totalRequests,
        long successfulRequests,
        long failedRequests,
        Instant lastFailureAt,
        Instant lastSuccessAt,
        List<ProviderMetricsSnapshot> modelMetrics
) {
    public ProviderHealthSnapshot {
        modelMetrics = modelMetrics == null ? List.of() : List.copyOf(modelMetrics);
    }
}
