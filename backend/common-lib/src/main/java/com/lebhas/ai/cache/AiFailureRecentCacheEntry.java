package com.lebhas.ai.cache;

import com.lebhas.ai.domain.AiFailureType;

import java.time.Instant;
import java.util.UUID;

public record AiFailureRecentCacheEntry(
        UUID failureLogId,
        UUID providerId,
        UUID creativeRequestId,
        UUID layerId,
        String modelName,
        AiFailureType failureType,
        String failureReason,
        int retryAttempt,
        boolean fallbackTriggered,
        Instant failedAt,
        Instant cachedAt
) {
    public AiFailureRecentCacheEntry {
        modelName = normalize(modelName);
        failureReason = normalize(failureReason);
        failedAt = failedAt == null ? Instant.now() : failedAt;
        cachedAt = cachedAt == null ? Instant.now() : cachedAt;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
