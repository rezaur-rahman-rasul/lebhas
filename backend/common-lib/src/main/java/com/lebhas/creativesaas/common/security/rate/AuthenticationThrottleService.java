package com.lebhas.creativesaas.common.security.rate;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.SecurityAuditLogger;
import com.lebhas.creativesaas.identity.domain.UserEntity;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthenticationThrottleService {

    private static final String REDIS_NAMESPACE = "creative-saas:rate-limits";

    private final StringRedisTemplate redisTemplate;
    private final AuthenticationRateLimitProperties properties;
    private final SecurityAuditLogger securityAuditLogger;
    private final Clock clock;

    public AuthenticationThrottleService(
            StringRedisTemplate redisTemplate,
            AuthenticationRateLimitProperties properties,
            SecurityAuditLogger securityAuditLogger,
            Clock clock
    ) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.securityAuditLogger = securityAuditLogger;
        this.clock = clock;
    }

    public void assertLoginAllowed(String email, String clientIp, UserEntity user) {
        Instant now = clock.instant();
        if (user != null && user.isLockedAt(now)) {
            securityAuditLogger.logRateLimitTriggered("login", normalizeEmail(email), normalizeIp(clientIp), "account_lock", user.getFailedLoginAttempts());
            throw new BusinessException(ErrorCode.AUTH_RATE_LIMITED, "Account is temporarily locked due to repeated failed login attempts");
        }

        long ipAttempts = readCounter(loginIpKey(clientIp));
        if (ipAttempts >= properties.getLogin().getMaxAttemptsPerIp()) {
            securityAuditLogger.logRateLimitTriggered("login", normalizeEmail(email), normalizeIp(clientIp), "ip", ipAttempts);
            throw new BusinessException(ErrorCode.AUTH_RATE_LIMITED);
        }

        long identityAttempts = readCounter(loginIdentityKey(email));
        if (identityAttempts >= properties.getLogin().getMaxAttemptsPerIdentity()) {
            securityAuditLogger.logRateLimitTriggered("login", normalizeEmail(email), normalizeIp(clientIp), "identity", identityAttempts);
            throw new BusinessException(ErrorCode.AUTH_RATE_LIMITED);
        }
    }

    public LoginFailureState recordLoginFailure(String email, String clientIp) {
        long identityAttempts = increment(loginIdentityKey(email), properties.getLogin().getWindow());
        long ipAttempts = increment(loginIpKey(clientIp), properties.getLogin().getWindow());
        Instant lockedUntil = identityAttempts >= properties.getLogin().getLockoutThreshold()
                ? clock.instant().plus(properties.getLogin().getLockoutDuration())
                : null;
        return new LoginFailureState(identityAttempts, ipAttempts, lockedUntil);
    }

    public void recordLoginSuccess(String email, String clientIp) {
        deleteQuietly(loginIdentityKey(email), loginIpKey(clientIp));
    }

    public void assertRefreshAllowed(String refreshToken, String clientIp) {
        long ipAttempts = increment(refreshIpKey(clientIp), properties.getRefresh().getWindow());
        if (ipAttempts > properties.getRefresh().getMaxAttemptsPerIp()) {
            securityAuditLogger.logRateLimitTriggered("refresh", tokenFingerprint(refreshToken), normalizeIp(clientIp), "ip", ipAttempts);
            throw new BusinessException(ErrorCode.AUTH_RATE_LIMITED);
        }

        long tokenAttempts = increment(refreshTokenKey(refreshToken), properties.getRefresh().getWindow());
        if (tokenAttempts > properties.getRefresh().getMaxAttemptsPerToken()) {
            securityAuditLogger.logRateLimitTriggered("refresh", tokenFingerprint(refreshToken), normalizeIp(clientIp), "token", tokenAttempts);
            throw new BusinessException(ErrorCode.AUTH_RATE_LIMITED);
        }
    }

    private long readCounter(String key) {
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(key))
                    .map(Long::parseLong)
                    .orElse(0L);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.AUTH_RATE_LIMITER_UNAVAILABLE);
        }
    }

    private long increment(String key, Duration ttl) {
        try {
            Long value = redisTemplate.opsForValue().increment(key);
            if (value == null) {
                throw new BusinessException(ErrorCode.AUTH_RATE_LIMITER_UNAVAILABLE);
            }
            if (value == 1L) {
                redisTemplate.expire(key, ttl);
            }
            return value;
        } catch (RuntimeException exception) {
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ErrorCode.AUTH_RATE_LIMITER_UNAVAILABLE);
        }
    }

    private void deleteQuietly(String... keys) {
        try {
            redisTemplate.delete(java.util.List.of(keys));
        } catch (RuntimeException ignored) {
        }
    }

    private String loginIpKey(String clientIp) {
        return REDIS_NAMESPACE + ":login:ip:" + normalizeIp(clientIp);
    }

    private String loginIdentityKey(String email) {
        return REDIS_NAMESPACE + ":login:identity:" + normalizeEmail(email);
    }

    private String refreshIpKey(String clientIp) {
        return REDIS_NAMESPACE + ":refresh:ip:" + normalizeIp(clientIp);
    }

    private String refreshTokenKey(String refreshToken) {
        return REDIS_NAMESPACE + ":refresh:token:" + tokenFingerprint(refreshToken);
    }

    private String normalizeEmail(String email) {
        return email == null || email.isBlank() ? "unknown" : email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String normalizeIp(String clientIp) {
        return clientIp == null || clientIp.isBlank() ? "unknown" : clientIp.trim();
    }

    private String tokenFingerprint(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return "missing";
        }
        String tokenId = rawToken.split("\\.", 2)[0];
        try {
            return UUID.fromString(tokenId).toString();
        } catch (IllegalArgumentException ignored) {
            return sha256(rawToken);
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is required", exception);
        }
    }

    public record LoginFailureState(long identityAttempts, long ipAttempts, Instant lockedUntil) {
    }
}
