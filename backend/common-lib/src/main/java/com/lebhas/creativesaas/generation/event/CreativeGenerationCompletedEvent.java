package com.lebhas.creativesaas.generation.event;

import com.lebhas.ai.provider.AiProviderType;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record CreativeGenerationCompletedEvent(
        String eventId,
        Instant occurredAt,
        UUID workspaceId,
        UUID creativeRequestId,
        UUID generatedVersionId,
        UUID creditReservationId,
        AiProviderType providerType,
        String providerName,
        String model,
        String providerJobId,
        UUID storageFileId,
        UUID assetId,
        String mimeType,
        Integer width,
        Integer height,
        Long duration,
        Map<String, Object> metadata,
        String message
) {
    public CreativeGenerationCompletedEvent {
        eventId = normalizeEventId(eventId);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        creativeRequestId = Objects.requireNonNull(creativeRequestId, "creativeRequestId must not be null");
        generatedVersionId = Objects.requireNonNull(generatedVersionId, "generatedVersionId must not be null");
        providerName = normalize(providerName);
        model = normalize(model);
        providerJobId = normalize(providerJobId);
        mimeType = normalize(mimeType);
        message = normalize(message);
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
