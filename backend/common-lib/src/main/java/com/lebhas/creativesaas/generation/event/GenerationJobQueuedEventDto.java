package com.lebhas.creativesaas.generation.event;

import java.time.Instant;
import java.util.UUID;

public record GenerationJobQueuedEventDto(
        UUID workspaceId,
        UUID creativeRequestId,
        UUID generationJobId,
        UUID creditReservationId,
        String queueName,
        Instant occurredAt
) {
    public GenerationJobQueuedEventDto {
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}
