package com.lebhas.creativesaas.generation.cache;

import com.lebhas.creativesaas.redis.RedisLockService;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class GenerationLockService {

    private final GenerationRedisKeys redisKeys;
    private final GenerationRedisAccessSupport redisAccessSupport;
    private final GenerationRedisTtlStrategy ttlStrategy;

    public GenerationLockService(
            GenerationRedisKeys redisKeys,
            GenerationRedisAccessSupport redisAccessSupport,
            GenerationRedisTtlStrategy ttlStrategy
    ) {
        this.redisKeys = redisKeys;
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<RedisLockService.RedisLockToken> acquire(UUID workspaceId, UUID creativeRequestId) {
        return redisAccessSupport.acquireLock(
                redisKeys.generationLock(creativeRequestId),
                ttlStrategy.generationLockTtl(),
                "generation_lock_acquire",
                GenerationRedisOperationContext.request(workspaceId, creativeRequestId));
    }

    public boolean release(
            RedisLockService.RedisLockToken token,
            UUID workspaceId,
            UUID creativeRequestId
    ) {
        return redisAccessSupport.releaseLock(
                token,
                "generation_lock_release",
                GenerationRedisOperationContext.request(workspaceId, creativeRequestId));
    }

    public boolean invalidate(UUID workspaceId, UUID creativeRequestId) {
        return redisAccessSupport.delete(
                redisKeys.generationLock(creativeRequestId),
                "generation_lock_delete",
                GenerationRedisOperationContext.request(workspaceId, creativeRequestId));
    }
}
