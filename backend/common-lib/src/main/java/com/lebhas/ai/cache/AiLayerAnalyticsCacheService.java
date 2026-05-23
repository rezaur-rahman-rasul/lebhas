package com.lebhas.ai.cache;

import java.util.Optional;
import java.util.UUID;

public class AiLayerAnalyticsCacheService {

    private final AiRedisAccessSupport redisAccessSupport;
    private final AiRedisTtlStrategy ttlStrategy;

    public AiLayerAnalyticsCacheService(AiRedisAccessSupport redisAccessSupport, AiRedisTtlStrategy ttlStrategy) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<AiLayerAnalyticsCacheEntry> get(UUID layerId, UUID providerId) {
        return redisAccessSupport.read(
                AiAnalyticsRedisKeys.layerAnalytics(layerId, providerId),
                AiLayerAnalyticsCacheEntry.class,
                "ai-layer-analytics-cache-read",
                new AiRedisOperationContext(null, null, null, providerId == null ? null : providerId.toString()));
    }

    public boolean store(AiLayerAnalyticsCacheEntry entry) {
        return redisAccessSupport.write(
                AiAnalyticsRedisKeys.layerAnalytics(entry.layerId(), entry.providerId()),
                entry,
                ttlStrategy.layerAnalyticsTtl(),
                "ai-layer-analytics-cache-write",
                new AiRedisOperationContext(null, null, null, entry.providerId() == null ? null : entry.providerId().toString()));
    }

    public boolean invalidate(UUID layerId, UUID providerId) {
        return redisAccessSupport.delete(
                AiAnalyticsRedisKeys.layerAnalytics(layerId, providerId),
                "ai-layer-analytics-cache-delete",
                new AiRedisOperationContext(null, null, null, providerId == null ? null : providerId.toString()));
    }
}
