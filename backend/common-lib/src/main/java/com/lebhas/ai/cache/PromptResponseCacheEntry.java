package com.lebhas.ai.cache;

import java.time.Instant;

public record PromptResponseCacheEntry(
        String promptHash,
        String provider,
        String model,
        String payload,
        Instant cachedAt
) {
}
