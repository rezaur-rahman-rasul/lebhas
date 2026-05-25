package com.lebhas.notification;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(
        name = "notification_preferences",
        schema = "platform",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_preferences_workspace_user_type",
                columnNames = {"workspace_id", "user_id", "notification_type"}
        ),
        indexes = {
                @Index(name = "idx_notification_preferences_workspace_user", columnList = "workspace_id,user_id"),
                @Index(name = "idx_notification_preferences_type", columnList = "notification_type")
        })
public class NotificationPreference extends TenantAwareEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "notification_type", nullable = false, length = 80)
    private String notificationType;

    @Column(name = "in_app_enabled", nullable = false)
    private boolean inAppEnabled;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled;

    @Column(name = "sms_enabled", nullable = false)
    private boolean smsEnabled;

    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled;

    protected NotificationPreference() {
    }

    public static NotificationPreference create(
            UUID workspaceId,
            UUID userId,
            String notificationType,
            boolean inAppEnabled,
            boolean emailEnabled,
            boolean smsEnabled,
            boolean pushEnabled
    ) {
        NotificationPreference preference = new NotificationPreference();
        preference.assignWorkspace(workspaceId);
        preference.userId = require(userId, "userId");
        preference.notificationType = normalizeRequired(notificationType, "notificationType");
        preference.inAppEnabled = inAppEnabled;
        preference.emailEnabled = emailEnabled;
        preference.smsEnabled = smsEnabled;
        preference.pushEnabled = pushEnabled;
        return preference;
    }

    public void update(boolean inAppEnabled, boolean emailEnabled, boolean smsEnabled, boolean pushEnabled) {
        this.inAppEnabled = inAppEnabled;
        this.emailEnabled = emailEnabled;
        this.smsEnabled = smsEnabled;
        this.pushEnabled = pushEnabled;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public boolean isInAppEnabled() {
        return inAppEnabled;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public boolean isSmsEnabled() {
        return smsEnabled;
    }

    public boolean isPushEnabled() {
        return pushEnabled;
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
