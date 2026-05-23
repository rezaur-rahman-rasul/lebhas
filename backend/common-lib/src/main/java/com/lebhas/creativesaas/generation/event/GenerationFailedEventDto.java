package com.lebhas.creativesaas.generation.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record GenerationFailedEventDto(
        UUID workspaceId,
        UUID creativeRequestId,
        UUID generationJobId,
        UUID generatedVersionId,
        UUID creditReservationId,
        BigDecimal refundedCredits,
        String failureReason,
        boolean retryable,
        boolean finalized,
        Instant occurredAt
) {
    public GenerationFailedEventDto {
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}
