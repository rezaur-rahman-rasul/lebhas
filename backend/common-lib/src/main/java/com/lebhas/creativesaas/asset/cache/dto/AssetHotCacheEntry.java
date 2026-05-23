package com.lebhas.creativesaas.asset.cache.dto;

import java.time.Instant;
import java.util.UUID;

public record AssetHotCacheEntry(
        UUID assetId,
        UUID workspaceId,
        long downloadCount,
        boolean hot,
        String lastDownloadType,
        Instant firstDownloadedAt,
        Instant lastDownloadedAt,
        Instant cachedAt
) {
}
