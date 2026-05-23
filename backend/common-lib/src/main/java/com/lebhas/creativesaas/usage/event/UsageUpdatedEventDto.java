package com.lebhas.creativesaas.usage.event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record UsageUpdatedEventDto(
        UUID workspaceId,
        LocalDate usageMonth,
        UUID referenceId,
        String referenceType,
        String updateType,
        Instant occurredAt
) {
    public UsageUpdatedEventDto {
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}
