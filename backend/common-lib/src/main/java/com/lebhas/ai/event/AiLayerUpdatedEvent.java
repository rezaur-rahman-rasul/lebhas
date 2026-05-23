package com.lebhas.ai.event;

import com.lebhas.ai.domain.CreativeLayerType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AiLayerUpdatedEvent(
        String eventId,
        Instant occurredAt,
        UUID pipelineId,
        UUID layerId,
        CreativeLayerType layerType,
        Integer layerOrder,
        Boolean enabled,
        UUID changedBy,
        String changeType,
        Map<String, Object> metadata
) {
    public AiLayerUpdatedEvent {
        eventId = AiPipelineEventSupport.eventId(eventId);
        occurredAt = AiPipelineEventSupport.occurredAt(occurredAt);
        changeType = AiPipelineEventSupport.normalize(changeType);
        metadata = AiPipelineEventSupport.immutable(metadata);
    }
}
