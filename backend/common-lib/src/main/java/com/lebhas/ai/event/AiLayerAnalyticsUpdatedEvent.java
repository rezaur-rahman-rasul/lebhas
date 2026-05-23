package com.lebhas.ai.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AiLayerAnalyticsUpdatedEvent(
        String eventId,
        Instant occurredAt,
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
        Map<String, Object> metadata
) {
    public AiLayerAnalyticsUpdatedEvent {
        eventId = AiMonitoringEventSupport.eventId(eventId);
        occurredAt = AiMonitoringEventSupport.occurredAt(occurredAt);
        modelName = AiMonitoringEventSupport.normalize(modelName);
        metadata = AiMonitoringEventSupport.immutable(metadata);
    }
}
