package com.lebhas.creativesaas.generation.event;

import com.lebhas.ai.provider.AiProviderType;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record CreativeGenerationFailedEvent(
        String eventId,
        Instant occurredAt,
        UUID workspaceId,
        UUID creativeRequestId,
        UUID generatedVersionId,
        UUID creditReservationId,
        AiProviderType providerType,
        String providerName,
        String model,
        String failureReason,
        boolean retryable,
        Map<String, Object> metadata
) {
    public CreativeGenerationFailedEvent {
        eventId = normalizeEventId(eventId);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        creativeRequestId = Objects.requireNonNull(creativeRequestId, "creativeRequestId must not be null");
        generatedVersionId = Objects.requireNonNull(generatedVersionId, "generatedVersionId must not be null");
        providerName = normalize(providerName);
        model = normalize(model);
        failureReason = normalize(failureReason);
        metadata = immutableCopy(metadata);
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

    private static Map<String, Object> immutableCopy(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(value));
    }
}
