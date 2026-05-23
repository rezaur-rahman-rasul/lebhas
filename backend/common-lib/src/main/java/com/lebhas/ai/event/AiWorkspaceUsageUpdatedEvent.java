package com.lebhas.ai.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AiWorkspaceUsageUpdatedEvent(
        String eventId,
        Instant occurredAt,
        UUID usageId,
        UUID workspaceId,
        long totalGenerationRequests,
        long totalGeneratedVersions,
        BigDecimal totalCreditsConsumed,
        BigDecimal totalEstimatedCostUsd,
        long totalFailures,
        BigDecimal avgGenerationTimeMs,
        Map<String, Object> metadata
) {
    public AiWorkspaceUsageUpdatedEvent {
        eventId = AiMonitoringEventSupport.eventId(eventId);
        occurredAt = AiMonitoringEventSupport.occurredAt(occurredAt);
        metadata = AiMonitoringEventSupport.immutable(metadata);
    }
}
