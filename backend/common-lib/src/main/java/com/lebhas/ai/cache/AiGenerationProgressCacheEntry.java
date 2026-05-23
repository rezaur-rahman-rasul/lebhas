package com.lebhas.ai.cache;

import com.lebhas.ai.job.AiJobState;

import java.time.Instant;
import java.util.UUID;

public record AiGenerationProgressCacheEntry(
        UUID workspaceId,
        UUID creativeRequestId,
        UUID jobId,
        AiJobState state,
        int progressPercent,
        String stage,
        String message,
        Instant updatedAt
) {
    public AiGenerationProgressCacheEntry {
        progressPercent = Math.max(0, Math.min(progressPercent, 100));
        stage = normalize(stage);
        message = normalize(message);
        updatedAt = updatedAt == null ? Instant.now() : updatedAt;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
