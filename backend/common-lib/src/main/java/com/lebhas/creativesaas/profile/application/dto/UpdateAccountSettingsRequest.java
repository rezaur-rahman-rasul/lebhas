package com.lebhas.creativesaas.profile.application.dto;

import com.lebhas.creativesaas.profile.domain.PreferredLanguage;
import com.lebhas.creativesaas.profile.domain.ThemePreference;
import jakarta.validation.constraints.NotNull;

public record UpdateAccountSettingsRequest(
        @NotNull
        PreferredLanguage preferredLanguage,
        @NotNull
        ThemePreference themePreference,
        boolean notificationEmailEnabled,
        boolean notificationInAppEnabled,
        boolean marketingEmailEnabled
) {
}
