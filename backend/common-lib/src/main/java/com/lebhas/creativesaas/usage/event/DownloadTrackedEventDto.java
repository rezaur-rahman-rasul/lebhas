package com.lebhas.creativesaas.usage.event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DownloadTrackedEventDto(
        UUID workspaceId,
        UUID downloadUsageLogId,
        UUID generatedVersionId,
        UUID assetId,
        UUID downloadedBy,
        String downloadType,
        LocalDate usageMonth,
        boolean summaryUpdated,
        Instant occurredAt
) {
    public DownloadTrackedEventDto {
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}
