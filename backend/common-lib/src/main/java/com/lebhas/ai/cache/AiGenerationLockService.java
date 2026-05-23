package com.lebhas.ai.cache;

import com.lebhas.creativesaas.redis.RedisLockService;

import java.util.Optional;
import java.util.UUID;

public class AiGenerationLockService {

    private final AiRedisAccessSupport redisAccessSupport;
    private final AiRedisTtlStrategy ttlStrategy;

    public AiGenerationLockService(AiRedisAccessSupport redisAccessSupport, AiRedisTtlStrategy ttlStrategy) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<RedisLockService.RedisLockToken> acquire(UUID workspaceId, UUID creativeRequestId) {
        return redisAccessSupport.acquireLock(
                AiRedisKeyConstants.pipelineGenerationLock(creativeRequestId),
                ttlStrategy.generationLockTtl(),
                "pipeline-generation-lock-acquire",
                AiRedisOperationContext.request(workspaceId, creativeRequestId));
    }

    public boolean release(UUID workspaceId, UUID creativeRequestId, RedisLockService.RedisLockToken token) {
        return redisAccessSupport.releaseLock(
                token,
                "pipeline-generation-lock-release",
                AiRedisOperationContext.request(workspaceId, creativeRequestId));
    }
}
