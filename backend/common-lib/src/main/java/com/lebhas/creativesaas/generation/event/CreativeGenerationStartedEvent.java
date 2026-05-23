package com.lebhas.creativesaas.generation.event;

import com.lebhas.ai.provider.AiProviderType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CreativeGenerationStartedEvent(
        String eventId,
        Instant occurredAt,
        UUID workspaceId,
        UUID creativeRequestId,
        UUID generatedVersionId,
        UUID creditReservationId,
        AiProviderType providerType,
        String model,
        int attempt
) {
    public CreativeGenerationStartedEvent {
        eventId = normalizeEventId(eventId);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        creativeRequestId = Objects.requireNonNull(creativeRequestId, "creativeRequestId must not be null");
        generatedVersionId = Objects.requireNonNull(generatedVersionId, "generatedVersionId must not be null");
        model = normalize(model);
        attempt = Math.max(0, attempt);
    }

    private static String normalizeEventId(String value) {
        if (value == null || value.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return value.trim();
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
