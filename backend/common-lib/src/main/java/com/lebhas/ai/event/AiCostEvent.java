package com.lebhas.ai.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AiCostEvent(
        String eventId,
        Instant occurredAt,
        UUID workspaceId,
        UUID creativeRequestId,
        UUID generatedVersionId,
        UUID generationJobId,
        UUID pipelineId,
        BigDecimal amount,
        String currency,
        Map<String, Object> breakdown,
        Map<String, Object> metadata
) {
    public AiCostEvent {
        eventId = AiPipelineEventSupport.eventId(eventId);
        occurredAt = AiPipelineEventSupport.occurredAt(occurredAt);
        currency = AiPipelineEventSupport.normalize(currency);
        breakdown = AiPipelineEventSupport.immutable(breakdown);
        metadata = AiPipelineEventSupport.immutable(metadata);
    }
}
