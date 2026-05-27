package com.lebhas.creativesaas.profile.application;

import com.lebhas.creativesaas.profile.application.dto.UserProfileView;
import com.lebhas.creativesaas.profile.domain.UserAccountSettings;
import org.springframework.stereotype.Component;

@Component
public class UserAccountSettingsMapper {

    public UserProfileView.AccountSettingsView toView(UserAccountSettings settings) {
        if (settings == null) {
            return null;
        }
        return new UserProfileView.AccountSettingsView(
                settings.getId(),
                settings.getPreferredLanguage(),
                settings.getThemePreference(),
                settings.isNotificationEmailEnabled(),
                settings.isNotificationInAppEnabled(),
                settings.isMarketingEmailEnabled(),
                settings.getCreatedAt(),
                settings.getUpdatedAt());
    }
}
