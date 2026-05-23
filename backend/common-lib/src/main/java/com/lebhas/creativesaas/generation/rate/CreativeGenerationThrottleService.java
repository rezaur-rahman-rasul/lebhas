package com.lebhas.creativesaas.generation.rate;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.generation.application.CreativeGenerationActivityLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class CreativeGenerationThrottleService {

    private final StringRedisTemplate redisTemplate;
    private final CreativeGenerationRateLimitProperties properties;
    private final CreativeGenerationActivityLogger activityLogger;
    private final String namespace;

    public CreativeGenerationThrottleService(
            StringRedisTemplate redisTemplate,
            CreativeGenerationRateLimitProperties properties,
            CreativeGenerationActivityLogger activityLogger,
            @Value("${platform.redis.rate-limit-namespace:creative-saas:rate-limits}") String namespace
    ) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.activityLogger = activityLogger;
        this.namespace = namespace;
    }

    public void assertAllowed(UUID workspaceId, UUID userId, String clientIp) {
        long ipAttempts = increment(ipKey(clientIp));
        if (ipAttempts > properties.getMaxRequestsPerIp()) {
            activityLogger.logRateLimitTriggered(workspaceId, userId, normalizeIp(clientIp), "ip", ipAttempts);
            throw new BusinessException(ErrorCode.GENERATION_RATE_LIMITED);
        }

        long workspaceAttempts = increment(workspaceKey(workspaceId));
        if (workspaceAttempts > properties.getMaxRequestsPerWorkspace()) {
            activityLogger.logRateLimitTriggered(workspaceId, userId, normalizeIp(clientIp), "workspace", workspaceAttempts);
            throw new BusinessException(ErrorCode.GENERATION_RATE_LIMITED);
        }

        long userAttempts = increment(userKey(userId));
        if (userAttempts > properties.getMaxRequestsPerUser()) {
            activityLogger.logRateLimitTriggered(workspaceId, userId, normalizeIp(clientIp), "user", userAttempts);
            throw new BusinessException(ErrorCode.GENERATION_RATE_LIMITED);
        }
    }

    private long increment(String key) {
        try {
            Long value = redisTemplate.opsForValue().increment(key);
            if (value == null) {
                throw new BusinessException(ErrorCode.GENERATION_RATE_LIMITER_UNAVAILABLE);
            }
            if (value == 1L) {
                redisTemplate.expire(key, properties.getWindow());
            }
            return value;
        } catch (RuntimeException exception) {
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ErrorCode.GENERATION_RATE_LIMITER_UNAVAILABLE);
        }
    }

    private String workspaceKey(UUID workspaceId) {
        return namespace + ":generation:workspace:" + workspaceId;
    }

    private String userKey(UUID userId) {
        return namespace + ":generation:user:" + userId;
    }

    private String ipKey(String clientIp) {
        return namespace + ":generation:ip:" + normalizeIp(clientIp);
    }

    private String normalizeIp(String clientIp) {
        return Optional.ofNullable(clientIp)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .orElse("unknown");
    }
}
