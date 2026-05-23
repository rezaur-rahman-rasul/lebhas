package com.lebhas.creativesaas.asset.cache.dto;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceStorageSummaryCacheEntry(
        UUID workspaceId,
        long totalBytesUsed,
        long totalUploads,
        long totalGeneratedAssets,
        long deletedAssetCount,
        long deletedBytesPendingCleanup,
        Instant lastCalculatedAt,
        Instant cachedAt
) {
}
