package com.lebhas.ai.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AiProviderMetricsUpdatedEvent(
        String eventId,
        Instant occurredAt,
        UUID providerId,
        String modelName,
        long totalRequests,
        long successfulRequests,
        long failedRequests,
        BigDecimal avgLatencyMs,
        BigDecimal avgCostUsd,
        BigDecimal avgQualityScore,
        BigDecimal uptimePercentage,
        Map<String, Object> metadata
) {
    public AiProviderMetricsUpdatedEvent {
        eventId = AiMonitoringEventSupport.eventId(eventId);
        occurredAt = AiMonitoringEventSupport.occurredAt(occurredAt);
        modelName = AiMonitoringEventSupport.normalize(modelName);
        metadata = AiMonitoringEventSupport.immutable(metadata);
    }
}
