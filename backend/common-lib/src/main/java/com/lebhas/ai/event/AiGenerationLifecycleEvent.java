package com.lebhas.ai.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AiGenerationLifecycleEvent(
        String eventId,
        Instant occurredAt,
        UUID workspaceId,
        UUID creativeRequestId,
        UUID generatedVersionId,
        UUID generationJobId,
        UUID pipelineId,
        String status,
        String reason,
        Map<String, Object> metadata
) {
    public AiGenerationLifecycleEvent {
        eventId = AiPipelineEventSupport.eventId(eventId);
        occurredAt = AiPipelineEventSupport.occurredAt(occurredAt);
        status = AiPipelineEventSupport.normalize(status);
        reason = AiPipelineEventSupport.normalize(reason);
        metadata = AiPipelineEventSupport.immutable(metadata);
    }
}
