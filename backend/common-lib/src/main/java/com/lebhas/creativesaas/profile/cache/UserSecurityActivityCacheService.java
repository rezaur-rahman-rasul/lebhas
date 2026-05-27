package com.lebhas.creativesaas.profile.cache;

import com.lebhas.creativesaas.profile.application.dto.SecurityActivityView;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserSecurityActivityCacheService {

    private final ProfileRedisAccessSupport redis;
    private final ProfileRedisTtlStrategy ttlStrategy;

    public UserSecurityActivityCacheService(ProfileRedisAccessSupport redis, ProfileRedisTtlStrategy ttlStrategy) {
        this.redis = redis;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<RecentSecurityActivityCacheEntry> getRecent(UUID userId) {
        return redis.read(
                ProfileRedisKeys.recentSecurityActivity(userId),
                RecentSecurityActivityCacheEntry.class,
                "profile_security_recent_get");
    }

    public boolean cacheRecent(UUID userId, List<SecurityActivityView> activities) {
        return redis.write(
                ProfileRedisKeys.recentSecurityActivity(userId),
                new RecentSecurityActivityCacheEntry(userId, activities == null ? List.of() : List.copyOf(activities)),
                ttlStrategy.securityActivityTtl(),
                "profile_security_recent_put");
    }

    public boolean invalidateRecent(UUID userId) {
        return redis.delete(ProfileRedisKeys.recentSecurityActivity(userId), "profile_security_recent_delete");
    }

    public record RecentSecurityActivityCacheEntry(UUID userId, List<SecurityActivityView> activities) {
    }
}
