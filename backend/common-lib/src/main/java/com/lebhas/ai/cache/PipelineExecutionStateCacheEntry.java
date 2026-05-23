package com.lebhas.ai.cache;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PipelineExecutionStateCacheEntry(
        UUID workspaceId,
        UUID creativeRequestId,
        UUID pipelineId,
        String state,
        int currentLayerOrder,
        String currentLayerType,
        Map<String, Object> metadata,
        Instant updatedAt
) {
    public PipelineExecutionStateCacheEntry {
        state = normalize(state);
        currentLayerType = normalize(currentLayerType);
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
