package com.lebhas.creativesaas.asset.storage;

import java.time.Duration;

public record SignedUrlRequest(
        String bucket,
        String objectKey,
        Duration ttl,
        String fileName,
        boolean download,
        String mimeType
) {
    public SignedUrlRequest {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("objectKey must not be blank");
        }
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
    }
}
