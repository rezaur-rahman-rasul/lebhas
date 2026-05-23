package com.lebhas.creativesaas.asset.cache.dto;

import java.time.Instant;
import java.util.UUID;

public record AssetSignedUrlCacheEntry(
        UUID assetId,
        UUID storageFileId,
        String type,
        String url,
        String cdnUrl,
        Instant generatedAt,
        Instant expiresAt
) {
}
