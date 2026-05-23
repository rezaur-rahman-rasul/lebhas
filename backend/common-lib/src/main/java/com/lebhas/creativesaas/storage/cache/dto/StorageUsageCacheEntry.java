package com.lebhas.creativesaas.storage.cache.dto;

import java.time.Instant;
import java.util.UUID;

public record StorageUsageCacheEntry(
        UUID workspaceId,
        long totalUsedBytes,
        long rawAssetBytes,
        long generatedAssetBytes,
        long variantBytes,
        long deletedBytes,
        Instant lastCalculatedAt,
        Instant cachedAt
) {
}
