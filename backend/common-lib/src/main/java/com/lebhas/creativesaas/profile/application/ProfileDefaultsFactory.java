package com.lebhas.creativesaas.profile.application;

import com.lebhas.creativesaas.identity.domain.UserEntity;
import com.lebhas.creativesaas.profile.domain.PreferredLanguage;
import com.lebhas.creativesaas.profile.domain.ThemePreference;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ProfileDefaultsFactory {

    public UserProfileDefaults userProfileDefaults(UserEntity user) {
        String firstName = requireText(user.getFirstName(), "firstName");
        String lastName = requireText(user.getLastName(), "lastName");
        String displayName = (firstName + " " + lastName).trim();
        return new UserProfileDefaults(
                user.getId(),
                firstName,
                lastName,
                displayName.isBlank() ? firstName : displayName,
                user.getPhone(),
                null,
                "Asia/Dhaka",
                "en");
    }

    public UserAccountSettingsDefaults accountSettingsDefaults(UserEntity user) {
        return new UserAccountSettingsDefaults(
                user.getId(),
                PreferredLanguage.BOTH,
                ThemePreference.SYSTEM,
                true,
                true,
                false);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    public record UserProfileDefaults(
            UUID userId,
            String firstName,
            String lastName,
            String displayName,
            String phoneNumber,
            String jobTitle,
            String timezone,
            String locale
    ) {
    }

    public record UserAccountSettingsDefaults(
            UUID userId,
            PreferredLanguage preferredLanguage,
            ThemePreference themePreference,
            boolean notificationEmailEnabled,
            boolean notificationInAppEnabled,
            boolean marketingEmailEnabled
    ) {
    }
}
