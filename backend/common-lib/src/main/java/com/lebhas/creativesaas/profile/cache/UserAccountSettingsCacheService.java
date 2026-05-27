package com.lebhas.creativesaas.profile.cache;

import com.lebhas.creativesaas.profile.application.dto.UserProfileView;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserAccountSettingsCacheService {

    private final ProfileRedisAccessSupport redis;
    private final ProfileRedisTtlStrategy ttlStrategy;

    public UserAccountSettingsCacheService(ProfileRedisAccessSupport redis, ProfileRedisTtlStrategy ttlStrategy) {
        this.redis = redis;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<UserProfileView.AccountSettingsView> get(UUID userId) {
        return redis.read(
                ProfileRedisKeys.accountSettings(userId),
                UserProfileView.AccountSettingsView.class,
                "profile_settings_get");
    }

    public boolean cache(UUID userId, UserProfileView.AccountSettingsView settings) {
        return redis.write(
                ProfileRedisKeys.accountSettings(userId),
                settings,
                ttlStrategy.accountSettingsTtl(),
                "profile_settings_put");
    }

    public boolean invalidate(UUID userId) {
        return redis.delete(ProfileRedisKeys.accountSettings(userId), "profile_settings_delete");
    }
}
