package com.lebhas.ai.cache;

import com.lebhas.ai.application.dto.LayerCostRecommendation;

import java.util.Optional;
import java.util.UUID;

public class AiRoutingRecommendationCacheService {

    private final AiRedisAccessSupport redisAccessSupport;
    private final AiRedisTtlStrategy ttlStrategy;

    public AiRoutingRecommendationCacheService(AiRedisAccessSupport redisAccessSupport, AiRedisTtlStrategy ttlStrategy) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<LayerCostRecommendation> get(UUID workspaceId, UUID layerId) {
        return redisAccessSupport.read(
                AiAnalyticsRedisKeys.routingRecommendation(workspaceId, layerId),
                LayerCostRecommendation.class,
                "ai-routing-recommendation-cache-read",
                new AiRedisOperationContext(workspaceId, null, null, null));
    }

    public boolean store(UUID workspaceId, LayerCostRecommendation recommendation) {
        return redisAccessSupport.write(
                AiAnalyticsRedisKeys.routingRecommendation(workspaceId, recommendation.layerId()),
                recommendation,
                ttlStrategy.routingRecommendationTtl(),
                "ai-routing-recommendation-cache-write",
                new AiRedisOperationContext(workspaceId, null, null, null));
    }

    public boolean invalidate(UUID workspaceId, UUID layerId) {
        return redisAccessSupport.delete(
                AiAnalyticsRedisKeys.routingRecommendation(workspaceId, layerId),
                "ai-routing-recommendation-cache-delete",
                new AiRedisOperationContext(workspaceId, null, null, null));
    }
}
