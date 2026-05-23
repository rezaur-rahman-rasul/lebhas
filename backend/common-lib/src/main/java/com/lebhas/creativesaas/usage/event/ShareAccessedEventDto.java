package com.lebhas.creativesaas.usage.event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ShareAccessedEventDto(
        UUID workspaceId,
        UUID shareUsageLogId,
        UUID shareLinkId,
        UUID generatedVersionId,
        UUID accessedByUserId,
        long accessCount,
        LocalDate usageMonth,
        boolean summaryUpdated,
        Instant occurredAt
) {
    public ShareAccessedEventDto {
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}
