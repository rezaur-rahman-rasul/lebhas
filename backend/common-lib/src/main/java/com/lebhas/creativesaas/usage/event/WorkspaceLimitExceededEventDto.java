package com.lebhas.creativesaas.usage.event;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceLimitExceededEventDto(
        UUID workspaceId,
        String limitType,
        String referenceType,
        UUID referenceId,
        String reason,
        Instant occurredAt
) {
    public WorkspaceLimitExceededEventDto {
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}
