package com.lebhas.creativesaas.pricing.cache;

import com.lebhas.creativesaas.redis.RedisCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class PricingRedisAccessSupport {

    private static final Logger log = LoggerFactory.getLogger(PricingRedisAccessSupport.class);

    private final RedisCacheService redisCacheService;

    public PricingRedisAccessSupport(RedisCacheService redisCacheService) {
        this.redisCacheService = redisCacheService;
    }

    public <T> Optional<T> read(String key, Class<T> type, String operation, PricingRedisOperationContext context) {
        try {
            return redisCacheService.get(key, type);
        } catch (RuntimeException exception) {
            logFailure(operation, key, context, exception);
            return Optional.empty();
        }
    }

    public boolean write(String key, Object value, Duration ttl, String operation, PricingRedisOperationContext context) {
        try {
            redisCacheService.set(key, value, ttl);
            return true;
        } catch (RuntimeException exception) {
            logFailure(operation, key, context, exception);
            return false;
        }
    }

    public boolean delete(String key, String operation, PricingRedisOperationContext context) {
        try {
            redisCacheService.delete(key);
            return true;
        } catch (RuntimeException exception) {
            logFailure(operation, key, context, exception);
            return false;
        }
    }

    private void logFailure(
            String operation,
            String key,
            PricingRedisOperationContext context,
            RuntimeException exception
    ) {
        String reason = exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage().replaceAll("\\s+", " ").trim();
        log.warn(
                "pricing_redis_failure operation={} key={} workspaceId={} pricingPlanId={} reason={}",
                operation,
                key,
                context == null ? null : context.workspaceId(),
                context == null ? null : context.pricingPlanId(),
                reason);
    }
}
