package com.lebhas.creativesaas.download.application.dto;

import java.time.Instant;
import java.util.UUID;

public record DownloadHistoryView(
        UUID id,
        UUID workspaceId,
        UUID generatedVersionId,
        UUID assetId,
        UUID downloadedBy,
        String downloadSource,
        String ipAddress,
        String userAgent,
        Instant downloadedAt
) {
}
