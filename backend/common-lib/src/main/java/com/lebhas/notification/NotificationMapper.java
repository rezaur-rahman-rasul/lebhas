package com.lebhas.notification;

import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationView toView(Notification notification) {
        return new NotificationView(
                notification.getId(),
                notification.getWorkspaceId(),
                notification.getRecipientUserId(),
                notification.getActorUserId(),
                notification.getNotificationType(),
                notification.getNotificationChannel(),
                notification.getNotificationPriority(),
                notification.getNotificationStatus(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReferenceType(),
                notification.getReferenceId(),
                notification.getSourceEventId(),
                notification.getReadAt(),
                notification.getCreatedAt());
    }

    public NotificationPreferenceView toView(NotificationPreference preference) {
        return new NotificationPreferenceView(
                preference.getId(),
                preference.getWorkspaceId(),
                preference.getUserId(),
                preference.getNotificationType(),
                preference.isInAppEnabled(),
                preference.isEmailEnabled(),
                preference.isSmsEnabled(),
                preference.isPushEnabled());
    }
}
