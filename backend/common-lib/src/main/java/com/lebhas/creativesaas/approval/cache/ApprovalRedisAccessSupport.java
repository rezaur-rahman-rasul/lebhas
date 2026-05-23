package com.lebhas.creativesaas.approval.cache;

import com.lebhas.creativesaas.redis.RedisCacheService;
import com.lebhas.creativesaas.redis.RedisLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class ApprovalRedisAccessSupport {

    private static final Logger log = LoggerFactory.getLogger(ApprovalRedisAccessSupport.class);

    private final RedisCacheService redisCacheService;
    private final RedisLockService redisLockService;

    public ApprovalRedisAccessSupport(
            RedisCacheService redisCacheService,
            RedisLockService redisLockService
    ) {
        this.redisCacheService = redisCacheService;
        this.redisLockService = redisLockService;
    }

    public <T> Optional<T> read(String key, Class<T> type, String operation, ApprovalRedisOperationContext context) {
        try {
            return redisCacheService.get(key, type);
        } catch (RuntimeException exception) {
            logFailure(operation, key, context, exception);
            return Optional.empty();
        }
    }

    public boolean write(String key, Object value, Duration ttl, String operation, ApprovalRedisOperationContext context) {
        try {
            redisCacheService.set(key, value, ttl);
            return true;
        } catch (RuntimeException exception) {
            logFailure(operation, key, context, exception);
            return false;
        }
    }

    public boolean delete(String key, String operation, ApprovalRedisOperationContext context) {
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
            ApprovalRedisOperationContext context
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
            ApprovalRedisOperationContext context
    ) {
        try {
            return redisLockService.release(token);
        } catch (RuntimeException exception) {
            logFailure(operation, token == null ? "unknown" : token.key(), context, exception);
            return false;
        }
    }

    private void logFailure(
            String operation,
            String key,
            ApprovalRedisOperationContext context,
            RuntimeException exception
    ) {
        String reason = exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage().replaceAll("\\s+", " ").trim();
        log.warn(
                "approval_redis_failure operation={} key={} workspaceId={} approvalRequestId={} approvalWorkflowId={} generatedVersionId={} reviewerId={} shareToken={} reason={}",
                operation,
                key,
                context == null ? null : context.workspaceId(),
                context == null ? null : context.approvalRequestId(),
                context == null ? null : context.approvalWorkflowId(),
                context == null ? null : context.generatedVersionId(),
                context == null ? null : context.reviewerId(),
                context == null ? null : context.shareToken(),
                reason);
    }
}
