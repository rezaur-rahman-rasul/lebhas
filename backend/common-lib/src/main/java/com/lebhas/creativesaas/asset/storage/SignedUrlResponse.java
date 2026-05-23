package com.lebhas.creativesaas.asset.storage;

import java.time.Instant;

public record SignedUrlResponse(
        String url,
        Instant expiresAt,
        String cdnUrl
) {
}
