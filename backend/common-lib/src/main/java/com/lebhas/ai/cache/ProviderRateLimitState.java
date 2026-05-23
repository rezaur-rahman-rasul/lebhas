package com.lebhas.ai.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record ProviderRateLimitState(
        String provider,
        UUID workspaceId,
        long currentCount,
        long limit,
        boolean allowed,
        Duration window,
        Instant observedAt,
        Instant expiresAt
) {
}
