package com.lebhas.ai.cache;

import com.lebhas.ai.application.dto.GenerationCostEstimate;

import java.util.Optional;
import java.util.UUID;

public class AiCostEstimateCacheService {

    private final AiRedisAccessSupport redisAccessSupport;
    private final AiRedisTtlStrategy ttlStrategy;

    public AiCostEstimateCacheService(AiRedisAccessSupport redisAccessSupport, AiRedisTtlStrategy ttlStrategy) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<GenerationCostEstimate> get(UUID workspaceId, String requestHash) {
        return redisAccessSupport.read(
                AiAnalyticsRedisKeys.costEstimate(workspaceId, requestHash),
                GenerationCostEstimate.class,
                "ai-cost-estimate-cache-read",
                new AiRedisOperationContext(workspaceId, null, null, null));
    }

    public boolean store(String requestHash, GenerationCostEstimate estimate) {
        return redisAccessSupport.write(
                AiAnalyticsRedisKeys.costEstimate(estimate.workspaceId(), requestHash),
                estimate,
                ttlStrategy.costEstimateAnalyticsTtl(),
                "ai-cost-estimate-cache-write",
                new AiRedisOperationContext(estimate.workspaceId(), estimate.creativeRequestId(), null, null));
    }

    public boolean invalidate(UUID workspaceId, String requestHash) {
        return redisAccessSupport.delete(
                AiAnalyticsRedisKeys.costEstimate(workspaceId, requestHash),
                "ai-cost-estimate-cache-delete",
                new AiRedisOperationContext(workspaceId, null, null, null));
    }
}
