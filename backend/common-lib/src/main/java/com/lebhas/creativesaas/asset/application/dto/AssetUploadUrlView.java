package com.lebhas.creativesaas.asset.application.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AssetUploadUrlView(
        UUID assetId,
        UUID uploadReferenceId,
        String uploadUrl,
        String method,
        Map<String, String> headers,
        Instant expiresAt,
        long maxFileSizeBytes
) {
}
