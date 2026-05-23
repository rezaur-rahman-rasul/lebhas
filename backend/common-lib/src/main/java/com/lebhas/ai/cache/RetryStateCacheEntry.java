package com.lebhas.ai.cache;

import com.lebhas.ai.domain.CreativeLayerType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record RetryStateCacheEntry(
        UUID workspaceId,
        UUID creativeRequestId,
        CreativeLayerType layerType,
        int attempt,
        int maxAttempts,
        Instant nextRetryAt,
        Map<String, Object> metadata,
        Instant updatedAt
) {
    public RetryStateCacheEntry {
        attempt = Math.max(0, attempt);
        maxAttempts = Math.max(0, maxAttempts);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        updatedAt = updatedAt == null ? Instant.now() : updatedAt;
    }
}
