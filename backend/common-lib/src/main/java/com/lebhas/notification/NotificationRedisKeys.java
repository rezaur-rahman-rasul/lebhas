package com.lebhas.notification;

import java.util.UUID;

public final class NotificationRedisKeys {

    private NotificationRedisKeys() {
    }

    public static String userNotifications(UUID userId) {
        return "notification:user:" + userId;
    }

    public static String userUnreadCount(UUID userId) {
        return "notification:unread:" + userId;
    }

    public static String userPreferences(UUID workspaceId, UUID userId) {
        return "notification:preference:" + workspaceId + ":" + userId;
    }

    public static String workspaceActivity(UUID workspaceId) {
        return "activity:workspace:" + workspaceId;
    }

    public static String workspaceTimeline(UUID workspaceId) {
        return "timeline:workspace:" + workspaceId;
    }

    public static String activeMonitoringAlerts() {
        return "monitoring:alerts:active";
    }

    public static String systemHealth() {
        return "monitoring:health:system";
    }

    public static String recentAudit(UUID workspaceId) {
        return "audit:recent:" + workspaceId;
    }
}
