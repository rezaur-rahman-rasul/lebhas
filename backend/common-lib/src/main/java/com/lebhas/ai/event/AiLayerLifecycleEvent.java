package com.lebhas.ai.event;

import com.lebhas.ai.domain.CreativeLayerType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AiLayerLifecycleEvent(
        String eventId,
        Instant occurredAt,
        UUID workspaceId,
        UUID creativeRequestId,
        UUID generatedVersionId,
        UUID generationJobId,
        UUID pipelineId,
        UUID layerId,
        CreativeLayerType layerType,
        UUID providerId,
        UUID fallbackProviderId,
        Integer attempt,
        String status,
        String reason,
        Map<String, Object> metadata
) {
    public AiLayerLifecycleEvent {
        eventId = AiPipelineEventSupport.eventId(eventId);
        occurredAt = AiPipelineEventSupport.occurredAt(occurredAt);
        status = AiPipelineEventSupport.normalize(status);
        reason = AiPipelineEventSupport.normalize(reason);
        metadata = AiPipelineEventSupport.immutable(metadata);
    }
}
