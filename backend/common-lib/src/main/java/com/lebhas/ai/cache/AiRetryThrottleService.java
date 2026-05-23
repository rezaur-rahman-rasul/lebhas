package com.lebhas.ai.cache;

import com.lebhas.creativesaas.redis.RedisRateLimitService;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class AiRetryThrottleService {

    private final AiRedisAccessSupport redisAccessSupport;
    private final AiRedisTtlStrategy ttlStrategy;

    public AiRetryThrottleService(
            AiRedisAccessSupport redisAccessSupport,
            AiRedisTtlStrategy ttlStrategy
    ) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<RetryThrottleState> recordRetry(UUID workspaceId, UUID creativeRequestId) {
        return recordRetry(workspaceId, creativeRequestId, 0, null);
    }

    public Optional<RetryThrottleState> recordRetry(
            UUID workspaceId,
            UUID creativeRequestId,
            int limitOverride,
            Duration windowOverride
    ) {
        int limit = ttlStrategy.retryThrottleLimit(limitOverride);
        Duration window = ttlStrategy.retryThrottleWindow(windowOverride);
        Optional<RedisRateLimitService.RateLimitWindow> rateLimitWindow = redisAccessSupport.incrementRateLimit(
                AiRedisKeyConstants.retryCreativeRequest(creativeRequestId),
                limit,
                window,
                "retry-throttle-increment",
                AiRedisOperationContext.request(workspaceId, creativeRequestId));
        if (rateLimitWindow.isEmpty()) {
            return Optional.empty();
        }
        RedisRateLimitService.RateLimitWindow value = rateLimitWindow.get();
        Instant observedAt = Instant.now();
        return Optional.of(new RetryThrottleState(
                workspaceId,
                creativeRequestId,
                value.currentCount(),
                value.limit(),
                value.allowed(),
                value.window(),
                observedAt,
                observedAt.plus(value.window())));
    }

    public boolean clear(UUID workspaceId, UUID creativeRequestId) {
        return redisAccessSupport.delete(
                AiRedisKeyConstants.retryCreativeRequest(creativeRequestId),
                "retry-throttle-delete",
                AiRedisOperationContext.request(workspaceId, creativeRequestId));
    }
}
