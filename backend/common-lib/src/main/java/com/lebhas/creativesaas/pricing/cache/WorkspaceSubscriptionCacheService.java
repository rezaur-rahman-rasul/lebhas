package com.lebhas.creativesaas.pricing.cache;

import com.lebhas.creativesaas.pricing.cache.dto.WorkspaceSubscriptionCacheEntry;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class WorkspaceSubscriptionCacheService {

    private final PricingRedisAccessSupport redisAccessSupport;
    private final PricingRedisTtlStrategy ttlStrategy;

    public WorkspaceSubscriptionCacheService(
            PricingRedisAccessSupport redisAccessSupport,
            PricingRedisTtlStrategy ttlStrategy
    ) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<WorkspaceSubscriptionCacheEntry> get(UUID workspaceId) {
        return redisAccessSupport.read(
                PricingRedisKeys.workspaceSubscription(workspaceId),
                WorkspaceSubscriptionCacheEntry.class,
                "workspace_subscription_read",
                PricingRedisOperationContext.workspace(workspaceId));
    }

    public WorkspaceSubscriptionCacheEntry getOrLoad(UUID workspaceId, Supplier<WorkspaceSubscriptionCacheEntry> loader) {
        return get(workspaceId).orElseGet(() -> {
            WorkspaceSubscriptionCacheEntry loaded = loader.get();
            if (loaded != null) {
                store(loaded);
            }
            return loaded;
        });
    }

    public void store(WorkspaceSubscriptionCacheEntry entry) {
        if (entry == null || entry.workspaceId() == null) {
            return;
        }
        redisAccessSupport.write(
                PricingRedisKeys.workspaceSubscription(entry.workspaceId()),
                entry,
                ttlStrategy.workspaceSubscriptionTtl(),
                "workspace_subscription_write",
                PricingRedisOperationContext.workspacePlan(entry.workspaceId(), entry.pricingPlanId()));
    }

    public void invalidate(UUID workspaceId) {
        redisAccessSupport.delete(
                PricingRedisKeys.workspaceSubscription(workspaceId),
                "workspace_subscription_delete",
                PricingRedisOperationContext.workspace(workspaceId));
    }
}
