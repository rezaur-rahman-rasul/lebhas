package com.lebhas.ai.application.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ProviderHealthSnapshotView(
        UUID id,
        UUID providerId,
        String status,
        int consecutiveFailures,
        boolean circuitOpen,
        Instant lastCheckedAt,
        String failureReason,
        Map<String, Object> metadata
) {
}
