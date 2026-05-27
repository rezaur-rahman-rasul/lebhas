package com.lebhas.creativesaas.profile.cache;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProfileImageUrlCacheService {

    private final ProfileRedisAccessSupport redis;
    private final ProfileRedisTtlStrategy ttlStrategy;

    public ProfileImageUrlCacheService(ProfileRedisAccessSupport redis, ProfileRedisTtlStrategy ttlStrategy) {
        this.redis = redis;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<ProfileImageUrlCacheEntry> get(UUID userId) {
        return redis.read(ProfileRedisKeys.profileImageUrl(userId), ProfileImageUrlCacheEntry.class, "profile_image_url_get");
    }

    public boolean cache(UUID userId, String imageUrl, Instant expiresAt) {
        return redis.write(
                ProfileRedisKeys.profileImageUrl(userId),
                new ProfileImageUrlCacheEntry(userId, imageUrl, expiresAt),
                ttlStrategy.profileImageUrlTtl(expiresAt),
                "profile_image_url_put");
    }

    public boolean invalidate(UUID userId) {
        return redis.delete(ProfileRedisKeys.profileImageUrl(userId), "profile_image_url_delete");
    }

    public record ProfileImageUrlCacheEntry(UUID userId, String imageUrl, Instant expiresAt) {
    }
}
