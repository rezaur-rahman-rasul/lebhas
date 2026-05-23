package com.lebhas.ai.cache;

import java.util.Optional;
import java.util.UUID;

public class AiCostEstimationCacheService {

    private final AiRedisAccessSupport redisAccessSupport;
    private final AiRedisTtlStrategy ttlStrategy;

    public AiCostEstimationCacheService(AiRedisAccessSupport redisAccessSupport, AiRedisTtlStrategy ttlStrategy) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<CostEstimationCacheEntry> get(UUID workspaceId, UUID creativeRequestId) {
        return redisAccessSupport.read(
                AiRedisKeyConstants.costEstimation(workspaceId, creativeRequestId),
                CostEstimationCacheEntry.class,
                "cost-estimation-cache-read",
                AiRedisOperationContext.request(workspaceId, creativeRequestId));
    }

    public boolean store(CostEstimationCacheEntry entry) {
        return redisAccessSupport.write(
                AiRedisKeyConstants.costEstimation(entry.workspaceId(), entry.creativeRequestId()),
                entry,
                ttlStrategy.costEstimationTtl(),
                "cost-estimation-cache-write",
                AiRedisOperationContext.request(entry.workspaceId(), entry.creativeRequestId()));
    }

    public boolean invalidate(UUID workspaceId, UUID creativeRequestId) {
        return redisAccessSupport.delete(
                AiRedisKeyConstants.costEstimation(workspaceId, creativeRequestId),
                "cost-estimation-cache-delete",
                AiRedisOperationContext.request(workspaceId, creativeRequestId));
    }
}
