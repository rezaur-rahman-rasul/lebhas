package com.lebhas.creativesaas.asset.cache.dto;

import java.time.Duration;
import java.time.Instant;

public record AssetRateLimitCacheEntry(
        String key,
        long currentCount,
        long limit,
        boolean allowed,
        Duration window,
        Instant observedAt,
        Instant resetsAt
) {
}
