package com.lebhas.creativesaas.asset.cache.dto;

import java.time.Instant;

public record UploadProgressCacheEntry(
        String uploadId,
        int progressPercentage,
        String uploadStatus,
        Instant updatedAt
) {
}
