package com.lebhas.ai.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AiQualityScoreCreatedEvent(
        String eventId,
        Instant occurredAt,
        UUID qualityScoreId,
        UUID workspaceId,
        UUID generatedVersionId,
        BigDecimal overallScore,
        BigDecimal textReadabilityScore,
        BigDecimal productPreservationScore,
        BigDecimal brandingScore,
        BigDecimal banglaTypographyScore,
        BigDecimal compositionScore,
        String qualityNotes,
        Map<String, Object> metadata
) {
    public AiQualityScoreCreatedEvent {
        eventId = AiMonitoringEventSupport.eventId(eventId);
        occurredAt = AiMonitoringEventSupport.occurredAt(occurredAt);
        qualityNotes = AiMonitoringEventSupport.normalize(qualityNotes);
        metadata = AiMonitoringEventSupport.immutable(metadata);
    }
}
