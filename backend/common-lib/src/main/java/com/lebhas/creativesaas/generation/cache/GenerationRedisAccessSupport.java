package com.lebhas.creativesaas.generation.cache;

import com.lebhas.creativesaas.redis.RedisCacheService;
import com.lebhas.creativesaas.redis.RedisLockService;
import com.lebhas.creativesaas.redis.RedisRateLimitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class GenerationRedisAccessSupport {

    private static final Logger log = LoggerFactory.getLogger(GenerationRedisAccessSupport.class);

    private final RedisCacheService redisCacheService;
    private final RedisLockService redisLockService;
    private final RedisRateLimitService redisRateLimitService;

    public GenerationRedisAccessSupport(
            RedisCacheService redisCacheService,
            RedisLockService redisLockService,
            RedisRateLimitService redisRateLimitService
    ) {
        this.redisCacheService = redisCacheService;
        this.redisLockService = redisLockService;
        this.redisRateLimitService = redisRateLimitService;
    }

    public <T> Optional<T> read(
            String key,
            Class<T> type,
            String operation,
            GenerationRedisOperationContext context
    ) {
        try {
            return redisCacheService.get(key, type);
        } catch (RuntimeException exception) {
            logFailure(operation, key, context, exception);
            return Optional.empty();
        }
    }

    public boolean write(
            String key,
            Object value,
            Duration ttl,
            String operation,
            GenerationRedisOperationContext context
    ) {
        try {
            redisCacheService.set(key, value, ttl);
            return true;
        } catch (RuntimeException exception) {
            logFailure(operation, key, context, exception);
            return false;
        }
    }

    public boolean delete(String key, String operation, GenerationRedisOperationContext context) {
        try {
            redisCacheService.delete(key);
            return true;
        } catch (RuntimeException exception) {
            logFailure(operation, key, context, exception);
            return false;
        }
    }

    public Optional<RedisLockService.RedisLockToken> acquireLock(
            String key,
            Duration ttl,
            String operation,
            GenerationRedisOperationContext context
    ) {
        try {
            return redisLockService.acquire(key, ttl);
        } catch (RuntimeException exception) {
            logFailure(operation, key, context, exception);
            return Optional.empty();
        }
    }

    public boolean releaseLock(
            RedisLockService.RedisLockToken token,
            String operation,
            GenerationRedisOperationContext context
    ) {
        if (token == null) {
            return false;
        }
        try {
            return redisLockService.release(token);
        } catch (RuntimeException exception) {
            logFailure(operation, token.key(), context, exception);
            return false;
        }
    }

    public Optional<RedisRateLimitService.RateLimitWindow> incrementRateLimit(
            String key,
            long limit,
            Duration window,
            String operation,
            GenerationRedisOperationContext context
    ) {
        try {
            return Optional.of(redisRateLimitService.increment(key, limit, window));
        } catch (RuntimeException exception) {
            logFailure(operation, key, context, exception);
            return Optional.empty();
        }
    }

    private void logFailure(
            String operation,
            String key,
            GenerationRedisOperationContext context,
            RuntimeException exception
    ) {
        String reason = exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage().replaceAll("\\s+", " ").trim();
        log.warn(
                "generation_redis_failure operation={} key={} workspaceId={} creativeRequestId={} jobId={} provider={} reason={}",
                operation,
                key,
                context == null ? null : context.workspaceId(),
                context == null ? null : context.creativeRequestId(),
                context == null ? null : context.jobId(),
                context == null ? null : context.provider(),
                reason);
    }
}
