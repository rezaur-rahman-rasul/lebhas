package com.lebhas.ai.cache;

import com.lebhas.ai.domain.CreativeLayerType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record LayerExecutionStateCacheEntry(
        UUID workspaceId,
        UUID creativeRequestId,
        UUID layerId,
        CreativeLayerType layerType,
        String state,
        int attempt,
        UUID providerId,
        UUID modelId,
        String message,
        Map<String, Object> metadata,
        Instant updatedAt
) {
    public LayerExecutionStateCacheEntry {
        state = normalize(state);
        message = normalize(message);
        attempt = Math.max(0, attempt);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        updatedAt = updatedAt == null ? Instant.now() : updatedAt;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
