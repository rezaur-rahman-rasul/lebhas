package com.lebhas.ai.event;

import com.lebhas.ai.domain.CreativeLayerType;
import com.lebhas.ai.domain.LayerRoutingStrategy;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AiRoutingResolvedEvent(
        String eventId,
        Instant occurredAt,
        UUID workspaceId,
        UUID creativeRequestId,
        UUID pipelineId,
        UUID layerId,
        CreativeLayerType layerType,
        UUID providerId,
        UUID modelId,
        UUID capabilityId,
        LayerRoutingStrategy routingStrategy,
        Map<String, Object> decisionMetadata
) {
    public AiRoutingResolvedEvent {
        eventId = AiPipelineEventSupport.eventId(eventId);
        occurredAt = AiPipelineEventSupport.occurredAt(occurredAt);
        decisionMetadata = AiPipelineEventSupport.immutable(decisionMetadata);
    }
}
