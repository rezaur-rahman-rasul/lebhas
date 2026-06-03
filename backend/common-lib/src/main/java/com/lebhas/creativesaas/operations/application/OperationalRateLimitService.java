package com.lebhas.creativesaas.operations.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.redis.RedisRateLimitService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
public class OperationalRateLimitService {
    public enum Operation {
        LOGIN, REGISTER, PASSWORD, UPLOAD, PROMPT, GENERATION, SHARE
    }

    private static final Map<Operation, Long> LIMITS = Map.of(
            Operation.LOGIN, 10L,
            Operation.REGISTER, 5L,
            Operation.PASSWORD, 5L,
            Operation.UPLOAD, 60L,
            Operation.PROMPT, 120L,
            Operation.GENERATION, 30L,
            Operation.SHARE, 60L);

    private final ObjectProvider<RedisRateLimitService> redisRateLimitService;

    public OperationalRateLimitService(ObjectProvider<RedisRateLimitService> redisRateLimitService) {
        this.redisRateLimitService = redisRateLimitService;
    }

    public RedisRateLimitService.RateLimitWindow check(Operation operation, String subject) {
        RedisRateLimitService service = redisRateLimitService.getIfAvailable();
        if (service == null) {
            return new RedisRateLimitService.RateLimitWindow(0, LIMITS.get(operation), true, Duration.ofMinutes(1));
        }
        RedisRateLimitService.RateLimitWindow window = service.increment("rate:%s:%s".formatted(operation.name().toLowerCase(), subject), LIMITS.get(operation), Duration.ofMinutes(1));
        if (!window.allowed()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Rate limit exceeded");
        }
        return window;
    }
}
