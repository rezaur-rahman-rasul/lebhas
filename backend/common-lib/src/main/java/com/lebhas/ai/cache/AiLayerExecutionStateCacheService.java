package com.lebhas.ai.cache;

import com.lebhas.ai.domain.CreativeLayerType;

import java.util.Optional;
import java.util.UUID;

public class AiLayerExecutionStateCacheService {

    private final AiRedisAccessSupport redisAccessSupport;
    private final AiRedisTtlStrategy ttlStrategy;

    public AiLayerExecutionStateCacheService(AiRedisAccessSupport redisAccessSupport, AiRedisTtlStrategy ttlStrategy) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<LayerExecutionStateCacheEntry> get(UUID workspaceId, UUID creativeRequestId, CreativeLayerType layerType) {
        return redisAccessSupport.read(
                AiRedisKeyConstants.layerState(creativeRequestId, layerType == null ? null : layerType.name()),
                LayerExecutionStateCacheEntry.class,
                "layer-execution-state-read",
                AiRedisOperationContext.request(workspaceId, creativeRequestId));
    }

    public boolean store(LayerExecutionStateCacheEntry entry) {
        return redisAccessSupport.write(
                AiRedisKeyConstants.layerState(entry.creativeRequestId(), entry.layerType() == null ? null : entry.layerType().name()),
                entry,
                ttlStrategy.layerExecutionStateTtl(),
                "layer-execution-state-write",
                AiRedisOperationContext.request(entry.workspaceId(), entry.creativeRequestId()));
    }

    public boolean clear(UUID workspaceId, UUID creativeRequestId, CreativeLayerType layerType) {
        return redisAccessSupport.delete(
                AiRedisKeyConstants.layerState(creativeRequestId, layerType == null ? null : layerType.name()),
                "layer-execution-state-delete",
                AiRedisOperationContext.request(workspaceId, creativeRequestId));
    }
}
