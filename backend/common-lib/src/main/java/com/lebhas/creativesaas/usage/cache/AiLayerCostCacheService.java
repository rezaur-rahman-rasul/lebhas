package com.lebhas.creativesaas.usage.cache;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class AiLayerCostCacheService {

    private final UsageBillingRedisKeys keys;
    private final UsageBillingRedisAccessSupport redis;
    private final UsageBillingRedisTtlStrategy ttl;

    public AiLayerCostCacheService(UsageBillingRedisKeys keys, UsageBillingRedisAccessSupport redis, UsageBillingRedisTtlStrategy ttl) {
        this.keys = keys;
        this.redis = redis;
        this.ttl = ttl;
    }

    public Optional<AiLayerCostSnapshot> get(UUID workspaceId, LocalDate month) {
        return redis.read(keys.aiLayerCost(workspaceId, month), AiLayerCostSnapshot.class, "ai_layer_cost_get",
                UsageBillingRedisOperationContext.of(workspaceId, month));
    }

    public boolean put(AiLayerCostSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        return redis.write(keys.aiLayerCost(snapshot.workspaceId(), snapshot.month()), snapshot, ttl.aiLayerCostTtl(),
                "ai_layer_cost_put", UsageBillingRedisOperationContext.of(snapshot.workspaceId(), snapshot.month()));
    }

    public boolean invalidate(UUID workspaceId, LocalDate month) {
        return redis.delete(keys.aiLayerCost(workspaceId, month), "ai_layer_cost_delete",
                UsageBillingRedisOperationContext.of(workspaceId, month));
    }

    public record AiLayerCostSnapshot(
            UUID workspaceId,
            LocalDate month,
            BigDecimal totalAiCostUsd,
            long totalLayerExecutions,
            Instant cachedAt
    ) {
    }
}
