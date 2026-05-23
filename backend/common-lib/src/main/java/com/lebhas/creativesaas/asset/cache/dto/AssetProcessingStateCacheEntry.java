package com.lebhas.creativesaas.asset.cache.dto;

import java.time.Instant;
import java.util.UUID;

public record AssetProcessingStateCacheEntry(
        UUID assetId,
        UUID workspaceId,
        String processingStatus,
        String previewStatus,
        boolean previewReady,
        boolean thumbnailReady,
        String coordinator,
        String jobId,
        String errorMessage,
        Instant startedAt,
        Instant completedAt,
        Instant updatedAt
) {
}
