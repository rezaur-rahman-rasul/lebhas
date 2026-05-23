package com.lebhas.creativesaas.usage.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record GenerationBlockedInsufficientCreditsEventDto(
        UUID workspaceId,
        UUID creativeRequestId,
        BigDecimal requiredCredits,
        BigDecimal availableCredits,
        Instant occurredAt
) {
    public GenerationBlockedInsufficientCreditsEventDto {
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}
