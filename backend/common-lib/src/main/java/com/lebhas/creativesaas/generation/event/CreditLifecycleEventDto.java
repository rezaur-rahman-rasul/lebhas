package com.lebhas.creativesaas.generation.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreditLifecycleEventDto(
        UUID workspaceId,
        UUID creativeRequestId,
        UUID generatedVersionId,
        UUID creditReservationId,
        BigDecimal amount,
        String status,
        String reason,
        Instant occurredAt
) {
    public CreditLifecycleEventDto {
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}
