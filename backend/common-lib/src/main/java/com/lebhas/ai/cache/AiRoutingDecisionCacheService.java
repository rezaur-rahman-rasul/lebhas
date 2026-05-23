package com.lebhas.ai.cache;

import com.lebhas.ai.domain.CreativeLayerType;

import java.util.Optional;
import java.util.UUID;

public class AiRoutingDecisionCacheService {

    private final AiRedisAccessSupport redisAccessSupport;
    private final AiRedisTtlStrategy ttlStrategy;

    public AiRoutingDecisionCacheService(AiRedisAccessSupport redisAccessSupport, AiRedisTtlStrategy ttlStrategy) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<RoutingDecisionCacheEntry> get(UUID workspaceId, CreativeLayerType layerType) {
        return redisAccessSupport.read(
                AiRedisKeyConstants.routing(workspaceId, layerType == null ? null : layerType.name()),
                RoutingDecisionCacheEntry.class,
                "routing-decision-cache-read",
                new AiRedisOperationContext(workspaceId, null, null, null));
    }

    public boolean store(RoutingDecisionCacheEntry entry) {
        return redisAccessSupport.write(
                AiRedisKeyConstants.routing(entry.workspaceId(), entry.layerType() == null ? null : entry.layerType().name()),
                entry,
                ttlStrategy.routingDecisionTtl(),
                "routing-decision-cache-write",
                new AiRedisOperationContext(entry.workspaceId(), null, null, null));
    }

    public boolean invalidate(UUID workspaceId, CreativeLayerType layerType) {
        return redisAccessSupport.delete(
                AiRedisKeyConstants.routing(workspaceId, layerType == null ? null : layerType.name()),
                "routing-decision-cache-delete",
                new AiRedisOperationContext(workspaceId, null, null, null));
    }
}
