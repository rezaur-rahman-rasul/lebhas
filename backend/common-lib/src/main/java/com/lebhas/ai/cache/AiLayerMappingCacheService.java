package com.lebhas.ai.cache;

import java.util.Optional;
import java.util.UUID;

public class AiLayerMappingCacheService {

    private final AiRedisAccessSupport redisAccessSupport;
    private final AiRedisTtlStrategy ttlStrategy;

    public AiLayerMappingCacheService(AiRedisAccessSupport redisAccessSupport, AiRedisTtlStrategy ttlStrategy) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<LayerMappingCacheEntry> get(UUID layerId) {
        return redisAccessSupport.read(
                AiRedisKeyConstants.layerMapping(layerId),
                LayerMappingCacheEntry.class,
                "layer-mapping-cache-read",
                null);
    }

    public boolean store(LayerMappingCacheEntry entry) {
        return redisAccessSupport.write(
                AiRedisKeyConstants.layerMapping(entry.layerId()),
                entry,
                ttlStrategy.layerDefinitionTtl(),
                "layer-mapping-cache-write",
                null);
    }

    public boolean invalidate(UUID layerId) {
        return redisAccessSupport.delete(
                AiRedisKeyConstants.layerMapping(layerId),
                "layer-mapping-cache-delete",
                null);
    }
}
