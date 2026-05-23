package com.lebhas.creativesaas.asset.cache;

import com.lebhas.creativesaas.asset.cache.dto.AssetRateLimitCacheEntry;
import com.lebhas.creativesaas.redis.RedisRateLimitService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AssetRateLimitRedisCacheService {

    private final AssetRedisAccessSupport redisAccessSupport;
    private final AssetCacheTtlStrategy ttlStrategy;

    public AssetRateLimitRedisCacheService(
            AssetRedisAccessSupport redisAccessSupport,
            AssetCacheTtlStrategy ttlStrategy
    ) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<AssetRateLimitCacheEntry> increment(
            String scope,
            String subject,
            String action,
            UUID workspaceId,
            UUID assetId,
            long limit,
            Duration window
    ) {
        Duration normalizedWindow = ttlStrategy.rateLimitWindow(window);
        String key = AssetCacheKeys.rateLimit(scope, subject, action);
        Optional<RedisRateLimitService.RateLimitWindow> rateLimitWindow = redisAccessSupport.incrementRateLimit(
                key,
                limit,
                normalizedWindow,
                workspaceId,
                assetId);
        if (rateLimitWindow.isEmpty()) {
            return Optional.empty();
        }
        RedisRateLimitService.RateLimitWindow value = rateLimitWindow.get();
        Instant observedAt = Instant.now();
        return Optional.of(new AssetRateLimitCacheEntry(
                key,
                value.currentCount(),
                value.limit(),
                value.allowed(),
                value.window(),
                observedAt,
                observedAt.plus(value.window())));
    }

    public void invalidate(
            String scope,
            String subject,
            String action,
            UUID workspaceId,
            UUID assetId
    ) {
        redisAccessSupport.delete(AssetCacheKeys.rateLimit(scope, subject, action), workspaceId, assetId);
    }
}
