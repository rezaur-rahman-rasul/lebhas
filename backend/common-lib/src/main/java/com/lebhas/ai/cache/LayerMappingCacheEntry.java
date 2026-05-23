package com.lebhas.ai.cache;

import com.lebhas.ai.application.dto.LayerToolMappingView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LayerMappingCacheEntry(
        UUID layerId,
        List<LayerToolMappingView> mappings,
        Instant cachedAt
) {
    public LayerMappingCacheEntry {
        mappings = mappings == null ? List.of() : List.copyOf(mappings);
        cachedAt = cachedAt == null ? Instant.now() : cachedAt;
    }
}
