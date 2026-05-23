package com.lebhas.creativesaas.creative.interfaces;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Signed preview URL for a generated version asset.")
public record GeneratedVersionPreviewUrlResponse(
        UUID generatedVersionId,
        String url,
        String type,
        String cdnUrl,
        boolean cached,
        Instant generatedAt,
        Instant expiresAt
) {
}
