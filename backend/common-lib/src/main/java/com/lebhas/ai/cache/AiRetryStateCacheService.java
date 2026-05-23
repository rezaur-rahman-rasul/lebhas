package com.lebhas.ai.cache;

import com.lebhas.ai.domain.CreativeLayerType;

import java.util.Optional;
import java.util.UUID;

public class AiRetryStateCacheService {

    private final AiRedisAccessSupport redisAccessSupport;
    private final AiRedisTtlStrategy ttlStrategy;

    public AiRetryStateCacheService(AiRedisAccessSupport redisAccessSupport, AiRedisTtlStrategy ttlStrategy) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<RetryStateCacheEntry> get(UUID workspaceId, UUID creativeRequestId, CreativeLayerType layerType) {
        return redisAccessSupport.read(
                AiRedisKeyConstants.retryState(creativeRequestId, layerType == null ? null : layerType.name()),
                RetryStateCacheEntry.class,
                "retry-state-read",
                AiRedisOperationContext.request(workspaceId, creativeRequestId));
    }

    public boolean store(RetryStateCacheEntry entry) {
        return redisAccessSupport.write(
                AiRedisKeyConstants.retryState(entry.creativeRequestId(), entry.layerType() == null ? null : entry.layerType().name()),
                entry,
                ttlStrategy.retryStateTtl(),
                "retry-state-write",
                AiRedisOperationContext.request(entry.workspaceId(), entry.creativeRequestId()));
    }

    public boolean clear(UUID workspaceId, UUID creativeRequestId, CreativeLayerType layerType) {
        return redisAccessSupport.delete(
                AiRedisKeyConstants.retryState(creativeRequestId, layerType == null ? null : layerType.name()),
                "retry-state-delete",
                AiRedisOperationContext.request(workspaceId, creativeRequestId));
    }
}
