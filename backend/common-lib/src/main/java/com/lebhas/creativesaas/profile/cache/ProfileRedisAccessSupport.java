package com.lebhas.creativesaas.profile.cache;

import com.lebhas.creativesaas.common.security.SecurityAuditLogger;
import com.lebhas.creativesaas.redis.RedisCacheService;
import com.lebhas.creativesaas.redis.RedisRateLimitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
class ProfileRedisAccessSupport {

    private static final Logger log = LoggerFactory.getLogger(ProfileRedisAccessSupport.class);

    private final RedisCacheService redisCacheService;
    private final RedisRateLimitService redisRateLimitService;
    private final SecurityAuditLogger securityAuditLogger;

    ProfileRedisAccessSupport(
            RedisCacheService redisCacheService,
            RedisRateLimitService redisRateLimitService,
            SecurityAuditLogger securityAuditLogger
    ) {
        this.redisCacheService = redisCacheService;
        this.redisRateLimitService = redisRateLimitService;
        this.securityAuditLogger = securityAuditLogger;
    }

    <T> Optional<T> read(String key, Class<T> type, String operation) {
        try {
            return redisCacheService.get(key, type);
        } catch (RuntimeException exception) {
            logFailure(operation, key, exception);
            return Optional.empty();
        }
    }

    boolean write(String key, Object value, Duration ttl, String operation) {
        try {
            redisCacheService.set(key, value, ttl);
            return true;
        } catch (RuntimeException exception) {
            logFailure(operation, key, exception);
            return false;
        }
    }

    boolean delete(String key, String operation) {
        try {
            redisCacheService.delete(key);
            return true;
        } catch (RuntimeException exception) {
            logFailure(operation, key, exception);
            return false;
        }
    }

    Optional<RedisRateLimitService.RateLimitWindow> incrementRateLimit(String key, long limit, Duration window, String operation) {
        try {
            return Optional.of(redisRateLimitService.increment(key, limit, window));
        } catch (RuntimeException exception) {
            logFailure(operation, key, exception);
            return Optional.empty();
        }
    }

    private void logFailure(String operation, String key, RuntimeException exception) {
        String reason = reason(exception);
        log.warn("profile_redis_failure operation={} key={} reason={}", operation, key, reason);
        securityAuditLogger.logRedisFailure(operation, reason);
    }

    private String reason(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage().replaceAll("\\s+", " ").trim();
    }
}
