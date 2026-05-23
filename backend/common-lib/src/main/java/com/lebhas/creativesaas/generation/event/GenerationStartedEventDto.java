package com.lebhas.creativesaas.generation.event;

import java.time.Instant;
import java.util.UUID;

public record GenerationStartedEventDto(
        UUID workspaceId,
        UUID creativeRequestId,
        UUID generationJobId,
        UUID creditReservationId,
        int attemptCount,
        Instant occurredAt
) {
    public GenerationStartedEventDto {
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}
