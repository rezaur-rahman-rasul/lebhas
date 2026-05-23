package com.lebhas.ai.cache;

import java.util.Optional;
import java.util.UUID;

public class AiFailureCacheService {

    private final AiRedisAccessSupport redisAccessSupport;
    private final AiRedisTtlStrategy ttlStrategy;

    public AiFailureCacheService(AiRedisAccessSupport redisAccessSupport, AiRedisTtlStrategy ttlStrategy) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<AiFailureRecentCacheEntry> getRecent(UUID providerId) {
        return redisAccessSupport.read(
                AiAnalyticsRedisKeys.recentFailure(providerId),
                AiFailureRecentCacheEntry.class,
                "ai-failure-recent-cache-read",
                new AiRedisOperationContext(null, null, null, providerId == null ? null : providerId.toString()));
    }

    public boolean storeRecent(AiFailureRecentCacheEntry entry) {
        return redisAccessSupport.write(
                AiAnalyticsRedisKeys.recentFailure(entry.providerId()),
                entry,
                ttlStrategy.failureRecentTtl(),
                "ai-failure-recent-cache-write",
                new AiRedisOperationContext(null, entry.creativeRequestId(), null, entry.providerId() == null ? null : entry.providerId().toString()));
    }

    public boolean invalidateRecent(UUID providerId) {
        return redisAccessSupport.delete(
                AiAnalyticsRedisKeys.recentFailure(providerId),
                "ai-failure-recent-cache-delete",
                new AiRedisOperationContext(null, null, null, providerId == null ? null : providerId.toString()));
    }
}
