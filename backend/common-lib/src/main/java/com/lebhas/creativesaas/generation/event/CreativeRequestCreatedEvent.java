package com.lebhas.creativesaas.generation.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CreativeRequestCreatedEvent(
        String eventId,
        Instant occurredAt,
        UUID workspaceId,
        UUID creativeRequestId,
        UUID projectCampaignId,
        UUID requestedBy,
        UUID creditReservationId,
        String status
) {
    public CreativeRequestCreatedEvent {
        eventId = normalizeEventId(eventId);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        creativeRequestId = Objects.requireNonNull(creativeRequestId, "creativeRequestId must not be null");
        projectCampaignId = Objects.requireNonNull(projectCampaignId, "projectCampaignId must not be null");
        requestedBy = Objects.requireNonNull(requestedBy, "requestedBy must not be null");
        status = normalize(status);
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
