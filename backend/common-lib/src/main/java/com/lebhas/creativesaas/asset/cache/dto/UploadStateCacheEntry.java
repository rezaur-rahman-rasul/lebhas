package com.lebhas.creativesaas.asset.cache.dto;

import java.time.Instant;
import java.util.UUID;

public record UploadStateCacheEntry(
        String uploadId,
        UUID uploadSessionId,
        UUID workspaceId,
        UUID projectId,
        UUID assetId,
        UUID uploadedBy,
        String hash,
        long totalBytes,
        long uploadedBytes,
        String uploadStatus,
        Instant createdAt,
        Instant updatedAt
) {
}
