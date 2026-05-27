package com.lebhas.creativesaas.profile.application.dto;

import com.lebhas.creativesaas.profile.domain.PreferredLanguage;
import com.lebhas.creativesaas.profile.domain.ThemePreference;

import java.time.Instant;
import java.util.UUID;

public record UserProfileView(
        UUID id,
        UUID userId,
        String firstName,
        String lastName,
        String displayName,
        String phoneNumber,
        String jobTitle,
        UUID profileImageAssetId,
        String profileImageUrl,
        Instant profileImageExpiresAt,
        String timezone,
        String locale,
        AccountSettingsView accountSettings,
        Instant createdAt,
        Instant updatedAt
) {
    public record AccountSettingsView(
            UUID id,
            PreferredLanguage preferredLanguage,
            ThemePreference themePreference,
            boolean notificationEmailEnabled,
            boolean notificationInAppEnabled,
            boolean marketingEmailEnabled,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
