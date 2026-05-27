package com.lebhas.creativesaas.profile.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(
        name = "user_account_settings",
        schema = "platform",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_account_settings_user_id", columnNames = "user_id")
)
public class UserAccountSettings extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_language", nullable = false, length = 20)
    private PreferredLanguage preferredLanguage;

    @Enumerated(EnumType.STRING)
    @Column(name = "theme_preference", nullable = false, length = 20)
    private ThemePreference themePreference;

    @Column(name = "notification_email_enabled", nullable = false)
    private boolean notificationEmailEnabled;

    @Column(name = "notification_in_app_enabled", nullable = false)
    private boolean notificationInAppEnabled;

    @Column(name = "marketing_email_enabled", nullable = false)
    private boolean marketingEmailEnabled;

    protected UserAccountSettings() {
    }

    public static UserAccountSettings create(
            UUID userId,
            PreferredLanguage preferredLanguage,
            ThemePreference themePreference,
            boolean notificationEmailEnabled,
            boolean notificationInAppEnabled,
            boolean marketingEmailEnabled
    ) {
        UserAccountSettings settings = new UserAccountSettings();
        settings.userId = requireUserId(userId);
        settings.preferredLanguage = preferredLanguage == null ? PreferredLanguage.ENGLISH : preferredLanguage;
        settings.themePreference = themePreference == null ? ThemePreference.SYSTEM : themePreference;
        settings.notificationEmailEnabled = notificationEmailEnabled;
        settings.notificationInAppEnabled = notificationInAppEnabled;
        settings.marketingEmailEnabled = marketingEmailEnabled;
        return settings;
    }

    public UUID getUserId() {
        return userId;
    }

    public PreferredLanguage getPreferredLanguage() {
        return preferredLanguage;
    }

    public ThemePreference getThemePreference() {
        return themePreference;
    }

    public boolean isNotificationEmailEnabled() {
        return notificationEmailEnabled;
    }

    public boolean isNotificationInAppEnabled() {
        return notificationInAppEnabled;
    }

    public boolean isMarketingEmailEnabled() {
        return marketingEmailEnabled;
    }

    public void update(
            PreferredLanguage preferredLanguage,
            ThemePreference themePreference,
            boolean notificationEmailEnabled,
            boolean notificationInAppEnabled,
            boolean marketingEmailEnabled
    ) {
        this.preferredLanguage = preferredLanguage == null ? PreferredLanguage.ENGLISH : preferredLanguage;
        this.themePreference = themePreference == null ? ThemePreference.SYSTEM : themePreference;
        this.notificationEmailEnabled = notificationEmailEnabled;
        this.notificationInAppEnabled = notificationInAppEnabled;
        this.marketingEmailEnabled = marketingEmailEnabled;
    }

    private static UUID requireUserId(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        return userId;
    }
}
