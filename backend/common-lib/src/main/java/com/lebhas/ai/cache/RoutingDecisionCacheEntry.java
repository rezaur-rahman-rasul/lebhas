package com.lebhas.ai.cache;

import com.lebhas.ai.domain.CreativeLayerType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record RoutingDecisionCacheEntry(
        UUID workspaceId,
        CreativeLayerType layerType,
        UUID pipelineId,
        UUID layerId,
        UUID providerId,
        UUID modelId,
        UUID capabilityId,
        String routingStrategy,
        Map<String, Object> decisionMetadata,
        Instant cachedAt
) {
    public RoutingDecisionCacheEntry {
        routingStrategy = normalize(routingStrategy);
        decisionMetadata = decisionMetadata == null ? Map.of() : Map.copyOf(decisionMetadata);
        cachedAt = cachedAt == null ? Instant.now() : cachedAt;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
