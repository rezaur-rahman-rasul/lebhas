package com.lebhas.ai.cache;

import java.time.Instant;
import java.util.UUID;

public record ActivePipelineCacheEntry(
        UUID pipelineId,
        String pipelineCode,
        int version,
        Instant cachedAt
) {
    public ActivePipelineCacheEntry {
        cachedAt = cachedAt == null ? Instant.now() : cachedAt;
    }
}
