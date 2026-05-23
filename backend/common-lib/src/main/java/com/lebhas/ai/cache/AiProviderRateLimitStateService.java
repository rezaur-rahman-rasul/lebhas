package com.lebhas.ai.cache;

import com.lebhas.creativesaas.redis.RedisRateLimitService;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class AiProviderRateLimitStateService {

    private final AiRedisAccessSupport redisAccessSupport;
    private final AiRedisTtlStrategy ttlStrategy;

    public AiProviderRateLimitStateService(
            AiRedisAccessSupport redisAccessSupport,
            AiRedisTtlStrategy ttlStrategy
    ) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<ProviderRateLimitState> increment(String provider, UUID workspaceId) {
        return increment(provider, workspaceId, 0, null);
    }

    public Optional<ProviderRateLimitState> increment(
            String provider,
            UUID workspaceId,
            int limitOverride,
            Duration windowOverride
    ) {
        int limit = ttlStrategy.providerRateLimit(limitOverride);
        Duration window = ttlStrategy.providerRateLimitWindow(windowOverride);
        Optional<RedisRateLimitService.RateLimitWindow> rateLimitWindow = redisAccessSupport.incrementRateLimit(
                AiRedisKeyConstants.providerRate(provider, workspaceId),
                limit,
                window,
                "provider-rate-limit-increment",
                new AiRedisOperationContext(workspaceId, null, null, provider));
        if (rateLimitWindow.isEmpty()) {
            return Optional.empty();
        }
        RedisRateLimitService.RateLimitWindow value = rateLimitWindow.get();
        Instant observedAt = Instant.now();
        return Optional.of(new ProviderRateLimitState(
                provider,
                workspaceId,
                value.currentCount(),
                value.limit(),
                value.allowed(),
                value.window(),
                observedAt,
                observedAt.plus(value.window())));
    }

    public boolean clear(String provider, UUID workspaceId) {
        return redisAccessSupport.delete(
                AiRedisKeyConstants.providerRate(provider, workspaceId),
                "provider-rate-limit-delete",
                new AiRedisOperationContext(workspaceId, null, null, provider));
    }
}
