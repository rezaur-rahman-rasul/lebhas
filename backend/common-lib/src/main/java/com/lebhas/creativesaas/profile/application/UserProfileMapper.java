package com.lebhas.creativesaas.profile.application;

import com.lebhas.creativesaas.identity.domain.UserEntity;
import com.lebhas.creativesaas.profile.application.dto.MasterUserProfileView;
import com.lebhas.creativesaas.profile.application.dto.UserProfileView;
import com.lebhas.creativesaas.profile.domain.UserAccountSettings;
import com.lebhas.creativesaas.profile.domain.UserProfile;
import org.springframework.stereotype.Component;

@Component
public class UserProfileMapper {

    private final UserAccountSettingsMapper userAccountSettingsMapper;

    public UserProfileMapper(UserAccountSettingsMapper userAccountSettingsMapper) {
        this.userAccountSettingsMapper = userAccountSettingsMapper;
    }

    public UserProfileView toView(UserProfile profile, UserAccountSettings settings) {
        return toView(profile, settings, null, null, null);
    }

    public UserProfileView toView(UserProfile profile, UserAccountSettings settings, String email) {
        return toView(profile, settings, email, null, null);
    }

    public UserProfileView toView(UserProfile profile, UserAccountSettings settings, String profileImageUrl, java.time.Instant profileImageExpiresAt) {
        return toView(profile, settings, null, profileImageUrl, profileImageExpiresAt);
    }

    public UserProfileView toView(
            UserProfile profile,
            UserAccountSettings settings,
            String email,
            String profileImageUrl,
            java.time.Instant profileImageExpiresAt
    ) {
        return new UserProfileView(
                profile.getId(),
                profile.getUserId(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getDisplayName(),
                email,
                profile.getPhoneNumber(),
                profile.getJobTitle(),
                profile.getProfileImageAssetId(),
                profileImageUrl,
                profileImageExpiresAt,
                profile.getTimezone(),
                profile.getLocale(),
                userAccountSettingsMapper.toView(settings),
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }

    public MasterUserProfileView toMasterView(
            UserEntity user,
            UserProfile profile,
            UserAccountSettings settings
    ) {
        return new MasterUserProfileView(
                user.getId(),
                profile == null ? null : profile.getId(),
                profile == null ? user.getFirstName() : profile.getFirstName(),
                profile == null ? user.getLastName() : profile.getLastName(),
                profile == null ? defaultDisplayName(user) : profile.getDisplayName(),
                maskEmail(user.getEmail()),
                maskPhone(profile == null ? user.getPhone() : profile.getPhoneNumber()),
                profile == null ? null : profile.getJobTitle(),
                profile == null ? null : profile.getProfileImageAssetId(),
                null,
                profile == null ? null : profile.getTimezone(),
                profile == null ? null : profile.getLocale(),
                user.getRole(),
                user.isMaster(),
                user.getStatus(),
                user.isEmailVerified(),
                settings == null ? null : settings.getPreferredLanguage(),
                settings == null ? null : settings.getThemePreference(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    private static String defaultDisplayName(UserEntity user) {
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }

    private static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String normalized = email.trim();
        int at = normalized.indexOf('@');
        if (at <= 0) {
            return maskMiddle(normalized, 1, 0);
        }
        String local = normalized.substring(0, at);
        String domain = normalized.substring(at);
        return maskMiddle(local, 1, local.length() > 4 ? 1 : 0) + domain;
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        return maskMiddle(phone.trim(), 3, 2);
    }

    private static String maskMiddle(String value, int visiblePrefix, int visibleSuffix) {
        if (value.length() <= visiblePrefix + visibleSuffix) {
            return "*".repeat(value.length());
        }
        String prefix = value.substring(0, Math.min(visiblePrefix, value.length()));
        String suffix = visibleSuffix == 0 ? "" : value.substring(value.length() - visibleSuffix);
        int maskedLength = Math.max(3, value.length() - prefix.length() - suffix.length());
        return prefix + "*".repeat(maskedLength) + suffix;
    }
}
