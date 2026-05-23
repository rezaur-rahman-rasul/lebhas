package com.lebhas.ai.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AiRoutingOptimizationRecommendedEvent(
        String eventId,
        Instant occurredAt,
        UUID workspaceId,
        UUID layerId,
        UUID currentProviderId,
        UUID recommendedProviderId,
        BigDecimal estimatedSavingsUsd,
        String reason,
        Map<String, Object> metadata
) {
    public AiRoutingOptimizationRecommendedEvent {
        eventId = AiMonitoringEventSupport.eventId(eventId);
        occurredAt = AiMonitoringEventSupport.occurredAt(occurredAt);
        reason = AiMonitoringEventSupport.normalize(reason);
        metadata = AiMonitoringEventSupport.immutable(metadata);
    }
}
