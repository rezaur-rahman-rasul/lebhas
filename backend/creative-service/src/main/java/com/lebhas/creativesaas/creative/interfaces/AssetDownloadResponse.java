package com.lebhas.creativesaas.creative.interfaces;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Signed download payload returned for an asset.")
public record AssetDownloadResponse(
        UUID assetId,
        String url,
        String type,
        String cdnUrl,
        boolean cached,
        Instant generatedAt,
        Instant expiresAt
) {
}
