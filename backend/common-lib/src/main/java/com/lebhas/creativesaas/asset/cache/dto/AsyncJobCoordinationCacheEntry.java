package com.lebhas.creativesaas.asset.cache.dto;

import java.time.Instant;
import java.util.UUID;

public record AsyncJobCoordinationCacheEntry(
        String jobId,
        String coordinationKey,
        UUID workspaceId,
        UUID assetId,
        String coordinator,
        String jobType,
        String status,
        int attempts,
        Instant queuedAt,
        Instant leasedAt,
        Instant completedAt,
        String errorMessage
) {
}
