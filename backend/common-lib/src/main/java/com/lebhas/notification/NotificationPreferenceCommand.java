package com.lebhas.notification;

import java.util.UUID;

public record NotificationPreferenceCommand(
        UUID workspaceId,
        UUID userId,
        String notificationType,
        boolean inAppEnabled,
        boolean emailEnabled,
        boolean smsEnabled,
        boolean pushEnabled
) {
}
