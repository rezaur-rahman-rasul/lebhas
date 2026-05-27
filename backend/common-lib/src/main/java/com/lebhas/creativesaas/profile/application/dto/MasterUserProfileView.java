package com.lebhas.creativesaas.profile.application.dto;

import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.identity.domain.UserStatus;
import com.lebhas.creativesaas.profile.domain.PreferredLanguage;
import com.lebhas.creativesaas.profile.domain.ThemePreference;

import java.time.Instant;
import java.util.UUID;

public record MasterUserProfileView(
        UUID userId,
        UUID profileId,
        String firstName,
        String lastName,
        String displayName,
        String maskedEmail,
        String maskedPhoneNumber,
        String jobTitle,
        UUID profileImageAssetId,
        String profileImageUrl,
        String timezone,
        String locale,
        Role role,
        boolean master,
        UserStatus status,
        boolean emailVerified,
        PreferredLanguage preferredLanguage,
        ThemePreference themePreference,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt
) {
}
