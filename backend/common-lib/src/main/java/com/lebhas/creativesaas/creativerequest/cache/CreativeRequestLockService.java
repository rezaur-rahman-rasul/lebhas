package com.lebhas.creativesaas.creativerequest.cache;

import com.lebhas.creativesaas.prompt.cache.PromptRedisAccessSupport;
import com.lebhas.creativesaas.prompt.cache.PromptRedisKeys;
import com.lebhas.creativesaas.prompt.cache.PromptRedisOperationContext;
import com.lebhas.creativesaas.prompt.cache.PromptRedisTtlStrategy;
import com.lebhas.creativesaas.redis.RedisLockService;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class CreativeRequestLockService {

    private final PromptRedisAccessSupport redisAccessSupport;
    private final PromptRedisTtlStrategy ttlStrategy;

    public CreativeRequestLockService(
            PromptRedisAccessSupport redisAccessSupport,
            PromptRedisTtlStrategy ttlStrategy
    ) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<RedisLockService.RedisLockToken> acquireProcessingLock(UUID workspaceId, UUID requestId) {
        return redisAccessSupport.acquireLock(
                PromptRedisKeys.requestProcessingLock(requestId),
                ttlStrategy.requestProcessingLockTtl(),
                "creative_request_processing_lock_acquire",
                PromptRedisOperationContext.creativeRequest(workspaceId, requestId));
    }

    public boolean releaseProcessingLock(
            RedisLockService.RedisLockToken token,
            UUID workspaceId,
            UUID requestId
    ) {
        return redisAccessSupport.releaseLock(
                token,
                "creative_request_processing_lock_release",
                PromptRedisOperationContext.creativeRequest(workspaceId, requestId));
    }
}
