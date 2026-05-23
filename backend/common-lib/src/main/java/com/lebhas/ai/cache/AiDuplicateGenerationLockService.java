package com.lebhas.ai.cache;

import com.lebhas.creativesaas.redis.RedisGenerationDeduplicationLock;
import com.lebhas.creativesaas.redis.RedisLockService;

import java.util.Optional;
import java.util.UUID;

public class AiDuplicateGenerationLockService {

    private final RedisGenerationDeduplicationLock deduplicationLock;
    private final AiRedisAccessSupport redisAccessSupport;
    private final AiRedisTtlStrategy ttlStrategy;

    public AiDuplicateGenerationLockService(
            RedisGenerationDeduplicationLock deduplicationLock,
            AiRedisAccessSupport redisAccessSupport,
            AiRedisTtlStrategy ttlStrategy
    ) {
        this.deduplicationLock = deduplicationLock;
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<RedisLockService.RedisLockToken> acquire(UUID workspaceId, UUID creativeRequestId, String requestHash) {
        try {
            return deduplicationLock.acquire(requestHash, ttlStrategy.duplicateGenerationLockTtl());
        } catch (RuntimeException exception) {
            return redisAccessSupport.acquireLock(
                    AiRedisKeyConstants.generationLock(requestHash),
                    ttlStrategy.duplicateGenerationLockTtl(),
                    "duplicate-generation-lock-acquire-fallback",
                    AiRedisOperationContext.request(workspaceId, creativeRequestId));
        }
    }

    public boolean release(UUID workspaceId, UUID creativeRequestId, RedisLockService.RedisLockToken token) {
        try {
            return deduplicationLock.release(token);
        } catch (RuntimeException exception) {
            return redisAccessSupport.releaseLock(
                    token,
                    "duplicate-generation-lock-release-fallback",
                    AiRedisOperationContext.request(workspaceId, creativeRequestId));
        }
    }
}
