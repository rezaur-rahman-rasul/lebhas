package com.lebhas.creativesaas.prompt.rate;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.prompt.application.PromptActivityLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class PromptThrottleService {

    private final StringRedisTemplate redisTemplate;
    private final PromptRateLimitProperties properties;
    private final PromptActivityLogger promptActivityLogger;
    private final String namespace;

    public PromptThrottleService(
            StringRedisTemplate redisTemplate,
            PromptRateLimitProperties properties,
            PromptActivityLogger promptActivityLogger,
            @Value("${platform.redis.rate-limit-namespace:creative-saas:rate-limits}") String namespace
    ) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.promptActivityLogger = promptActivityLogger;
        this.namespace = namespace;
    }

    public void assertAllowed(UUID workspaceId, UUID userId, String clientIp, String operation) {
        long ipAttempts = increment(ipKey(clientIp));
        if (ipAttempts > properties.getMaxRequestsPerIp()) {
            promptActivityLogger.logRateLimitTriggered(operation, workspaceId, userId, normalizeIp(clientIp), "ip", ipAttempts);
            throw new BusinessException(ErrorCode.PROMPT_RATE_LIMITED);
        }

        long workspaceAttempts = increment(workspaceKey(workspaceId));
        if (workspaceAttempts > properties.getMaxRequestsPerWorkspace()) {
            promptActivityLogger.logRateLimitTriggered(operation, workspaceId, userId, normalizeIp(clientIp), "workspace", workspaceAttempts);
            throw new BusinessException(ErrorCode.PROMPT_RATE_LIMITED);
        }

        long userAttempts = increment(userKey(userId));
        if (userAttempts > properties.getMaxRequestsPerUser()) {
            promptActivityLogger.logRateLimitTriggered(operation, workspaceId, userId, normalizeIp(clientIp), "user", userAttempts);
            throw new BusinessException(ErrorCode.PROMPT_RATE_LIMITED);
        }
    }

    private long increment(String key) {
        try {
            Long value = redisTemplate.opsForValue().increment(key);
            if (value == null) {
                throw new BusinessException(ErrorCode.PROMPT_RATE_LIMITER_UNAVAILABLE);
            }
            if (value == 1L) {
                redisTemplate.expire(key, properties.getWindow());
            }
            return value;
        } catch (RuntimeException exception) {
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ErrorCode.PROMPT_RATE_LIMITER_UNAVAILABLE);
        }
    }

    private String workspaceKey(UUID workspaceId) {
        return namespace + ":prompt:workspace:" + workspaceId;
    }

    private String userKey(UUID userId) {
        return namespace + ":prompt:user:" + userId;
    }

    private String ipKey(String clientIp) {
        return namespace + ":prompt:ip:" + normalizeIp(clientIp);
    }

    private String normalizeIp(String clientIp) {
        return Optional.ofNullable(clientIp)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .orElse("unknown");
    }
}
