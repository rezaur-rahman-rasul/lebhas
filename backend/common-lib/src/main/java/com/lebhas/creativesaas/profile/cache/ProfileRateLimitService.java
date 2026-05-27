package com.lebhas.creativesaas.profile.cache;

import com.lebhas.creativesaas.redis.RedisRateLimitService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class ProfileRateLimitService {

    private static final long DEFAULT_PROFILE_UPDATE_LIMIT = 20L;
    private static final long DEFAULT_PASSWORD_CHANGE_LIMIT = 5L;

    private final ProfileRedisAccessSupport redis;
    private final ProfileRedisTtlStrategy ttlStrategy;

    public ProfileRateLimitService(ProfileRedisAccessSupport redis, ProfileRedisTtlStrategy ttlStrategy) {
        this.redis = redis;
        this.ttlStrategy = ttlStrategy;
    }

    public RateLimitDecision incrementProfileUpdate(UUID userId) {
        return incrementProfileUpdate(userId, DEFAULT_PROFILE_UPDATE_LIMIT, ttlStrategy.profileUpdateRateWindow());
    }

    public RateLimitDecision incrementProfileUpdate(UUID userId, long limit, Duration window) {
        return increment(ProfileRedisKeys.profileUpdateRateLimit(userId), limit, window, "profile_update_rate_limit");
    }

    public RateLimitDecision incrementPasswordChange(UUID userId) {
        return incrementPasswordChange(userId, DEFAULT_PASSWORD_CHANGE_LIMIT, ttlStrategy.passwordRateWindow());
    }

    public RateLimitDecision incrementPasswordChange(UUID userId, long limit, Duration window) {
        return increment(ProfileRedisKeys.profilePasswordRateLimit(userId), limit, window, "profile_password_rate_limit");
    }

    private RateLimitDecision increment(String key, long limit, Duration window, String operation) {
        Duration resolvedWindow = ttlStrategy.positive(window, ttlStrategy.profileUpdateRateWindow());
        return redis.incrementRateLimit(key, normalizeLimit(limit), resolvedWindow, operation)
                .map(this::toDecision)
                .orElseGet(() -> RateLimitDecision.redisUnavailable(normalizeLimit(limit), resolvedWindow));
    }

    private RateLimitDecision toDecision(RedisRateLimitService.RateLimitWindow window) {
        return new RateLimitDecision(
                window.currentCount(),
                window.limit(),
                window.allowed(),
                window.window(),
                false);
    }

    private long normalizeLimit(long limit) {
        return limit <= 0 ? 1 : limit;
    }

    public record RateLimitDecision(
            long currentCount,
            long limit,
            boolean allowed,
            Duration window,
            boolean redisUnavailable
    ) {
        static RateLimitDecision redisUnavailable(long limit, Duration window) {
            return new RateLimitDecision(0, limit, true, window, true);
        }
    }
}
