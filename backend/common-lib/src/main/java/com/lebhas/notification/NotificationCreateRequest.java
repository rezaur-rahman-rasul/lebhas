package com.lebhas.notification;

import java.util.Objects;
import java.util.UUID;

public record NotificationCreateRequest(
        UUID workspaceId,
        UUID recipientUserId,
        UUID actorUserId,
        NotificationType notificationType,
        String title,
        String message,
        String referenceType,
        UUID referenceId,
        String sourceEventId
) {

    public NotificationCreateRequest {
        workspaceId = Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        recipientUserId = Objects.requireNonNull(recipientUserId, "recipientUserId must not be null");
        actorUserId = Objects.requireNonNull(actorUserId, "actorUserId must not be null");
        notificationType = Objects.requireNonNull(notificationType, "notificationType must not be null");
        referenceId = Objects.requireNonNull(referenceId, "referenceId must not be null");
        title = normalizeRequired(title, "title");
        message = normalizeRequired(message, "message");
        referenceType = normalizeRequired(referenceType, "referenceType");
        sourceEventId = normalizeRequired(sourceEventId, "sourceEventId");
    }

    private static String normalizeRequired(String value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return trimmed;
    }
}
