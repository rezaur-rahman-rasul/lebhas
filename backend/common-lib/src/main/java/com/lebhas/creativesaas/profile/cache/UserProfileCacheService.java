package com.lebhas.creativesaas.profile.cache;

import com.lebhas.creativesaas.profile.application.dto.UserProfileView;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserProfileCacheService {

    private final ProfileRedisAccessSupport redis;
    private final ProfileRedisTtlStrategy ttlStrategy;

    public UserProfileCacheService(ProfileRedisAccessSupport redis, ProfileRedisTtlStrategy ttlStrategy) {
        this.redis = redis;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<UserProfileView> get(UUID userId) {
        return redis.read(ProfileRedisKeys.userProfile(userId), UserProfileView.class, "profile_user_get");
    }

    public boolean cache(UUID userId, UserProfileView profile) {
        return redis.write(ProfileRedisKeys.userProfile(userId), profile, ttlStrategy.userProfileTtl(), "profile_user_put");
    }

    public boolean invalidate(UUID userId) {
        return redis.delete(ProfileRedisKeys.userProfile(userId), "profile_user_delete");
    }
}
