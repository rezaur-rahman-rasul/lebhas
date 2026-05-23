package com.lebhas.ai.cache;

import com.lebhas.creativesaas.redis.RedisLockService;

import java.util.Optional;
import java.util.UUID;

public class AiCreditReservationLockService {

    private final AiRedisAccessSupport redisAccessSupport;
    private final AiRedisTtlStrategy ttlStrategy;

    public AiCreditReservationLockService(
            AiRedisAccessSupport redisAccessSupport,
            AiRedisTtlStrategy ttlStrategy
    ) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<RedisLockService.RedisLockToken> acquire(UUID workspaceId) {
        return redisAccessSupport.acquireLock(
                AiRedisKeyConstants.creditReservationLock(workspaceId),
                ttlStrategy.creditReservationLockTtl(),
                "credit-reservation-lock-acquire",
                AiRedisOperationContext.workspace(workspaceId));
    }

    public boolean release(UUID workspaceId, RedisLockService.RedisLockToken token) {
        return redisAccessSupport.releaseLock(
                token,
                "credit-reservation-lock-release",
                AiRedisOperationContext.workspace(workspaceId));
    }
}
