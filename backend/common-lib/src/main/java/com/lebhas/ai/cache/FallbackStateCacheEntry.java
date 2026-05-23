package com.lebhas.ai.cache;

import com.lebhas.ai.domain.CreativeLayerType;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record FallbackStateCacheEntry(
        UUID workspaceId,
        UUID creativeRequestId,
        CreativeLayerType layerType,
        UUID failedProviderId,
        UUID activeFallbackProviderId,
        int fallbackAttempt,
        List<UUID> attemptedProviderIds,
        Map<String, Object> metadata,
        Instant updatedAt
) {
    public FallbackStateCacheEntry {
        fallbackAttempt = Math.max(0, fallbackAttempt);
        attemptedProviderIds = attemptedProviderIds == null ? List.of() : List.copyOf(attemptedProviderIds);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        updatedAt = updatedAt == null ? Instant.now() : updatedAt;
    }
}
