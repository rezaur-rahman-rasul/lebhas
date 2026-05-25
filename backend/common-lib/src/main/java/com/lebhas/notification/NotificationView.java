package com.lebhas.notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationView(
        UUID id,
        UUID workspaceId,
        UUID recipientUserId,
        UUID actorUserId,
        NotificationType notificationType,
        NotificationChannel notificationChannel,
        NotificationPriority notificationPriority,
        NotificationStatus notificationStatus,
        String title,
        String message,
        String referenceType,
        UUID referenceId,
        String sourceEventId,
        Instant readAt,
        Instant createdAt
) {
}
