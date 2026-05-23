package com.lebhas.creativesaas.asset.cache;

import com.lebhas.creativesaas.asset.application.AssetActivityLogger;
import com.lebhas.creativesaas.redis.RedisCacheService;
import com.lebhas.creativesaas.redis.RedisRateLimitService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
public class AssetRedisAccessSupport {

    private final RedisCacheService redisCacheService;
    private final RedisRateLimitService redisRateLimitService;
    private final StringRedisTemplate redisTemplate;
    private final AssetActivityLogger assetActivityLogger;

    public AssetRedisAccessSupport(
            RedisCacheService redisCacheService,
            RedisRateLimitService redisRateLimitService,
            StringRedisTemplate redisTemplate,
            AssetActivityLogger assetActivityLogger
    ) {
        this.redisCacheService = redisCacheService;
        this.redisRateLimitService = redisRateLimitService;
        this.redisTemplate = redisTemplate;
        this.assetActivityLogger = assetActivityLogger;
    }

    public <T> Optional<T> read(String key, Class<T> type, UUID workspaceId, UUID assetId) {
        try {
            return redisCacheService.get(key, type);
        } catch (RuntimeException exception) {
            logFailure(key, workspaceId, assetId, exception);
            return Optional.empty();
        }
    }

    public boolean write(String key, Object value, Duration ttl, UUID workspaceId, UUID assetId) {
        try {
            redisCacheService.set(key, value, ttl);
            return true;
        } catch (RuntimeException exception) {
            logFailure(key, workspaceId, assetId, exception);
            return false;
        }
    }

    public boolean delete(String key, UUID workspaceId, UUID assetId) {
        try {
            redisCacheService.delete(key);
            return true;
        } catch (RuntimeException exception) {
            logFailure(key, workspaceId, assetId, exception);
            return false;
        }
    }

    public long deleteByPattern(String pattern, UUID workspaceId, UUID assetId) {
        try {
            return redisCacheService.deleteByPattern(pattern);
        } catch (RuntimeException exception) {
            logFailure(pattern, workspaceId, assetId, exception);
            return 0L;
        }
    }

    public Optional<Long> increment(String key, Duration ttl, UUID workspaceId, UUID assetId) {
        try {
            Long current = redisTemplate.opsForValue().increment(key);
            if (current != null && ttl != null && !ttl.isNegative() && !ttl.isZero() && current == 1L) {
                redisTemplate.expire(key, ttl);
            }
            return Optional.ofNullable(current);
        } catch (RuntimeException exception) {
            logFailure(key, workspaceId, assetId, exception);
            return Optional.empty();
        }
    }

    public Optional<RedisRateLimitService.RateLimitWindow> incrementRateLimit(
            String key,
            long limit,
            Duration window,
            UUID workspaceId,
            UUID assetId
    ) {
        try {
            return Optional.of(redisRateLimitService.increment(key, limit, window));
        } catch (RuntimeException exception) {
            logFailure(key, workspaceId, assetId, exception);
            return Optional.empty();
        }
    }

    public boolean putIfAbsent(String key, String value, Duration ttl, UUID workspaceId, UUID assetId) {
        try {
            Boolean stored = redisTemplate.opsForValue().setIfAbsent(key, value, ttl);
            return Boolean.TRUE.equals(stored);
        } catch (RuntimeException exception) {
            logFailure(key, workspaceId, assetId, exception);
            return false;
        }
    }

    private void logFailure(String key, UUID workspaceId, UUID assetId, RuntimeException exception) {
        String reason = exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage().replaceAll("\\s+", " ").trim();
        assetActivityLogger.logRedisFailure(key, workspaceId, assetId, reason);
    }
}
