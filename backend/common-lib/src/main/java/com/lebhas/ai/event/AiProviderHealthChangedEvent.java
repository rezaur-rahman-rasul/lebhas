package com.lebhas.ai.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AiProviderHealthChangedEvent(
        String eventId,
        Instant occurredAt,
        UUID providerId,
        String healthStatus,
        BigDecimal reliabilityScore,
        BigDecimal uptimePercentage,
        long totalRequests,
        long failedRequests,
        Map<String, Object> metadata
) {
    public AiProviderHealthChangedEvent {
        eventId = AiMonitoringEventSupport.eventId(eventId);
        occurredAt = AiMonitoringEventSupport.occurredAt(occurredAt);
        healthStatus = AiMonitoringEventSupport.normalize(healthStatus);
        metadata = AiMonitoringEventSupport.immutable(metadata);
    }
}
