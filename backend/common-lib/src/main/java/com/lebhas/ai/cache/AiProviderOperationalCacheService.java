package com.lebhas.ai.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.lebhas.creativesaas.redis.RedisRateLimitService;

public class AiProviderOperationalCacheService {

    private final AiRedisAccessSupport redisAccessSupport;
    private final AiRedisTtlStrategy ttlStrategy;

    public AiProviderOperationalCacheService(AiRedisAccessSupport redisAccessSupport, AiRedisTtlStrategy ttlStrategy) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<ProviderStatusCacheEntry> getStatus(UUID providerId) {
        return redisAccessSupport.read(
                AiRedisKeyConstants.provider(providerId),
                ProviderStatusCacheEntry.class,
                "provider-status-cache-read",
                null);
    }

    public boolean storeStatus(ProviderStatusCacheEntry entry) {
        return redisAccessSupport.write(
                AiRedisKeyConstants.provider(entry.providerId()),
                entry,
                ttlStrategy.providerStatusTtl(),
                "provider-status-cache-write",
                null);
    }

    public boolean invalidateStatus(UUID providerId) {
        return redisAccessSupport.delete(
                AiRedisKeyConstants.provider(providerId),
                "provider-status-cache-delete",
                null);
    }

    public Optional<ProviderRateLimitState> incrementRate(UUID providerId, UUID workspaceId, int limitOverride, Duration windowOverride) {
        int limit = ttlStrategy.providerRateLimit(limitOverride);
        Duration window = ttlStrategy.providerRateLimitWindow(windowOverride);
        Optional<RedisRateLimitService.RateLimitWindow> rateLimitWindow = redisAccessSupport.incrementRateLimit(
                AiRedisKeyConstants.providerRate(providerId, workspaceId),
                limit,
                window,
                "provider-rate-limit-increment",
                new AiRedisOperationContext(workspaceId, null, null, providerId == null ? null : providerId.toString()));
        if (rateLimitWindow.isEmpty()) {
            return Optional.empty();
        }
        RedisRateLimitService.RateLimitWindow value = rateLimitWindow.get();
        Instant observedAt = Instant.now();
        return Optional.of(new ProviderRateLimitState(
                providerId == null ? null : providerId.toString(),
                workspaceId,
                value.currentCount(),
                value.limit(),
                value.allowed(),
                value.window(),
                observedAt,
                observedAt.plus(value.window())));
    }

    public boolean clearRate(UUID providerId, UUID workspaceId) {
        return redisAccessSupport.delete(
                AiRedisKeyConstants.providerRate(providerId, workspaceId),
                "provider-rate-limit-delete",
                new AiRedisOperationContext(workspaceId, null, null, providerId == null ? null : providerId.toString()));
    }
}
