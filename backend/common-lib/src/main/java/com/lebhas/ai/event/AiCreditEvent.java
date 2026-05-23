package com.lebhas.ai.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AiCreditEvent(
        String eventId,
        Instant occurredAt,
        UUID workspaceId,
        UUID creativeRequestId,
        UUID generatedVersionId,
        UUID creditReservationId,
        BigDecimal amount,
        String currency,
        String status,
        String reason,
        Map<String, Object> metadata
) {
    public AiCreditEvent {
        eventId = AiPipelineEventSupport.eventId(eventId);
        occurredAt = AiPipelineEventSupport.occurredAt(occurredAt);
        currency = AiPipelineEventSupport.normalize(currency);
        status = AiPipelineEventSupport.normalize(status);
        reason = AiPipelineEventSupport.normalize(reason);
        metadata = AiPipelineEventSupport.immutable(metadata);
    }
}
