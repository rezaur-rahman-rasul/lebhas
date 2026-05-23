package com.lebhas.ai.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record RetryThrottleState(
        UUID workspaceId,
        UUID creativeRequestId,
        long currentCount,
        long limit,
        boolean allowed,
        Duration window,
        Instant observedAt,
        Instant expiresAt
) {
}
