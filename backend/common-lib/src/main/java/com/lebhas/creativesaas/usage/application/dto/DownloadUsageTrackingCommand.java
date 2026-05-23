package com.lebhas.creativesaas.usage.application.dto;

import java.time.LocalDate;
import java.util.UUID;

public record DownloadUsageTrackingCommand(
        UUID workspaceId,
        UUID generatedVersionId,
        UUID assetId,
        UUID downloadedBy,
        String downloadType,
        String ipAddress,
        String userAgent,
        LocalDate usageMonth
) {
}
