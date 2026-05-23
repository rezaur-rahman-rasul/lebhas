package com.lebhas.creativesaas.asset.application.dto;

import java.time.Instant;

public record AssetUrlView(
        String url,
        String type,
        String cdnUrl,
        boolean cached,
        Instant generatedAt,
        Instant expiresAt
) {
}
