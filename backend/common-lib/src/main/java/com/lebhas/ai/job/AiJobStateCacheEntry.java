package com.lebhas.ai.job;

import com.lebhas.ai.provider.AiProviderType;

import java.time.Instant;
import java.util.UUID;

public record AiJobStateCacheEntry(
        UUID workspaceId,
        UUID creativeRequestId,
        UUID jobId,
        AiProviderType providerType,
        String model,
        AiJobState state,
        int attempt,
        String providerJobId,
        String message,
        Instant updatedAt
) {
    public AiJobStateCacheEntry {
        attempt = Math.max(0, attempt);
        model = normalize(model);
        providerJobId = normalize(providerJobId);
        message = normalize(message);
        updatedAt = updatedAt == null ? Instant.now() : updatedAt;
    }

    public static AiJobStateCacheEntry queued(
            UUID workspaceId,
            UUID creativeRequestId,
            UUID jobId,
            AiProviderType providerType,
            String model,
            int attempt
    ) {
        return new AiJobStateCacheEntry(
                workspaceId,
                creativeRequestId,
                jobId,
                providerType,
                model,
                AiJobState.QUEUED,
                attempt,
                null,
                null,
                Instant.now());
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
