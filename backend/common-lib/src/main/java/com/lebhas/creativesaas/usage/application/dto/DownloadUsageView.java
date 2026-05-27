package com.lebhas.creativesaas.usage.application.dto;

import com.lebhas.creativesaas.profile.application.dto.SafeProfileDisplayView;

import java.time.Instant;
import java.util.UUID;

public record DownloadUsageView(
        UUID id,
        UUID workspaceId,
        UUID generatedVersionId,
        UUID assetId,
        UUID downloadedBy,
        SafeProfileDisplayView downloadedByDisplay,
        String downloadType,
        String ipAddress,
        String userAgent,
        Instant createdAt
) {
}
