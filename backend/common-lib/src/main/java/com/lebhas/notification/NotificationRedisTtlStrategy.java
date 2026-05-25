package com.lebhas.notification;

import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class NotificationRedisTtlStrategy {

    public Duration userNotificationsTtl() {
        return Duration.ofMinutes(5);
    }

    public Duration unreadCountTtl() {
        return Duration.ofMinutes(2);
    }

    public Duration preferenceTtl() {
        return Duration.ofMinutes(15);
    }

    public Duration activityFeedTtl() {
        return Duration.ofMinutes(5);
    }

    public Duration workspaceTimelineTtl() {
        return Duration.ofMinutes(5);
    }

    public Duration monitoringAlertsTtl() {
        return Duration.ofMinutes(1);
    }

    public Duration systemHealthTtl() {
        return Duration.ofMinutes(1);
    }

    public Duration auditRecentTtl() {
        return Duration.ofMinutes(5);
    }
}
