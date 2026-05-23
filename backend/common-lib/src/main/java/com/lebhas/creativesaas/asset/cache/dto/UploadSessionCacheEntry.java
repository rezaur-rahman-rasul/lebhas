package com.lebhas.creativesaas.asset.cache.dto;

import java.time.Instant;
import java.util.UUID;

public record UploadSessionCacheEntry(
        UUID uploadSessionId,
        UUID workspaceId,
        UUID projectId,
        UUID assetId,
        UUID uploadedBy,
        String originalFileName,
        String mimeType,
        long fileSize,
        String hash,
        int chunkCount,
        int completedChunkCount,
        String status,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
}
