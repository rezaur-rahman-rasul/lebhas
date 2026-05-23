package com.lebhas.ai.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AiPipelineUpdatedEvent(
        String eventId,
        Instant occurredAt,
        UUID pipelineId,
        String pipelineCode,
        Integer version,
        UUID changedBy,
        String changeType,
        Map<String, Object> metadata
) {
    public AiPipelineUpdatedEvent {
        eventId = AiPipelineEventSupport.eventId(eventId);
        occurredAt = AiPipelineEventSupport.occurredAt(occurredAt);
        pipelineCode = AiPipelineEventSupport.normalize(pipelineCode);
        changeType = AiPipelineEventSupport.normalize(changeType);
        metadata = AiPipelineEventSupport.immutable(metadata);
    }
}
