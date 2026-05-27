package com.lebhas.creativesaas.profile.cache;

import com.lebhas.creativesaas.common.security.SecurityAuditLogger;
import com.lebhas.creativesaas.redis.RedisLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProfileLockService {

    private static final Logger log = LoggerFactory.getLogger(ProfileLockService.class);

    private final RedisLockService redisLockService;
    private final ProfileRedisTtlStrategy ttlStrategy;
    private final SecurityAuditLogger securityAuditLogger;

    public ProfileLockService(
            RedisLockService redisLockService,
            ProfileRedisTtlStrategy ttlStrategy,
            SecurityAuditLogger securityAuditLogger
    ) {
        this.redisLockService = redisLockService;
        this.ttlStrategy = ttlStrategy;
        this.securityAuditLogger = securityAuditLogger;
    }

    public Optional<RedisLockService.RedisLockToken> acquireProfileUpdateLock(UUID userId) {
        return acquire(ProfileRedisKeys.lockProfileUpdate(userId), ttlStrategy.lockTtl(), "profile_update_lock_acquire");
    }

    public Optional<RedisLockService.RedisLockToken> acquirePasswordLock(UUID userId) {
        return acquire(ProfileRedisKeys.lockProfilePassword(userId), ttlStrategy.lockTtl(), "profile_password_lock_acquire");
    }

    public Optional<RedisLockService.RedisLockToken> acquireProfileImageLock(UUID userId) {
        return acquire(ProfileRedisKeys.lockProfileImage(userId), ttlStrategy.lockTtl(), "profile_image_lock_acquire");
    }

    public Optional<RedisLockService.RedisLockToken> acquireSessionLock(UUID userId) {
        return acquire(ProfileRedisKeys.lockProfileSession(userId), ttlStrategy.sessionLockTtl(), "profile_session_lock_acquire");
    }

    public boolean release(RedisLockService.RedisLockToken token) {
        if (token == null) {
            return false;
        }
        try {
            return redisLockService.release(token);
        } catch (RuntimeException exception) {
            logFailure("profile_lock_release", token.key(), exception);
            return false;
        }
    }

    public boolean releaseQuietly(RedisLockService.RedisLockToken token) {
        if (token == null) {
            return false;
        }
        try {
            return redisLockService.releaseQuietly(token);
        } catch (RuntimeException exception) {
            logFailure("profile_lock_release_quietly", token.key(), exception);
            return false;
        }
    }

    private Optional<RedisLockService.RedisLockToken> acquire(String key, Duration ttl, String operation) {
        try {
            return redisLockService.acquire(key, ttl);
        } catch (RuntimeException exception) {
            logFailure(operation, key, exception);
            return Optional.empty();
        }
    }

    private void logFailure(String operation, String key, RuntimeException exception) {
        String reason = reason(exception);
        log.warn("profile_redis_lock_failure operation={} key={} reason={}", operation, key, reason);
        securityAuditLogger.logRedisFailure(operation, reason);
    }

    private String reason(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage().replaceAll("\\s+", " ").trim();
    }
}
