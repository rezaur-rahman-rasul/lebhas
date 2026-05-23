package com.lebhas.ai.cache;

import com.lebhas.ai.domain.CreativeLayerType;

import java.util.Optional;
import java.util.UUID;

public class AiFallbackStateCacheService {

    private final AiRedisAccessSupport redisAccessSupport;
    private final AiRedisTtlStrategy ttlStrategy;

    public AiFallbackStateCacheService(AiRedisAccessSupport redisAccessSupport, AiRedisTtlStrategy ttlStrategy) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<FallbackStateCacheEntry> get(UUID workspaceId, UUID creativeRequestId, CreativeLayerType layerType) {
        return redisAccessSupport.read(
                AiRedisKeyConstants.fallbackState(creativeRequestId, layerType == null ? null : layerType.name()),
                FallbackStateCacheEntry.class,
                "fallback-state-read",
                AiRedisOperationContext.request(workspaceId, creativeRequestId));
    }

    public boolean store(FallbackStateCacheEntry entry) {
        return redisAccessSupport.write(
                AiRedisKeyConstants.fallbackState(entry.creativeRequestId(), entry.layerType() == null ? null : entry.layerType().name()),
                entry,
                ttlStrategy.fallbackStateTtl(),
                "fallback-state-write",
                AiRedisOperationContext.request(entry.workspaceId(), entry.creativeRequestId()));
    }

    public boolean clear(UUID workspaceId, UUID creativeRequestId, CreativeLayerType layerType) {
        return redisAccessSupport.delete(
                AiRedisKeyConstants.fallbackState(creativeRequestId, layerType == null ? null : layerType.name()),
                "fallback-state-delete",
                AiRedisOperationContext.request(workspaceId, creativeRequestId));
    }
}
