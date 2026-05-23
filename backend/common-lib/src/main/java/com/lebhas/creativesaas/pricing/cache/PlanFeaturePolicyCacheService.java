package com.lebhas.creativesaas.pricing.cache;

import com.lebhas.creativesaas.pricing.cache.dto.PlanFeaturePolicyCacheEntry;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class PlanFeaturePolicyCacheService {

    private final PricingRedisAccessSupport redisAccessSupport;
    private final PricingRedisTtlStrategy ttlStrategy;

    public PlanFeaturePolicyCacheService(
            PricingRedisAccessSupport redisAccessSupport,
            PricingRedisTtlStrategy ttlStrategy
    ) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<PlanFeaturePolicyCacheEntry> get(UUID planId) {
        return redisAccessSupport.read(
                PricingRedisKeys.planFeatures(planId),
                PlanFeaturePolicyCacheEntry.class,
                "plan_feature_policy_read",
                PricingRedisOperationContext.plan(planId));
    }

    public PlanFeaturePolicyCacheEntry getOrLoad(UUID planId, Supplier<PlanFeaturePolicyCacheEntry> loader) {
        return get(planId).orElseGet(() -> {
            PlanFeaturePolicyCacheEntry loaded = loader.get();
            if (loaded != null) {
                store(loaded);
            }
            return loaded;
        });
    }

    public void store(PlanFeaturePolicyCacheEntry entry) {
        if (entry == null || entry.pricingPlanId() == null) {
            return;
        }
        redisAccessSupport.write(
                PricingRedisKeys.planFeatures(entry.pricingPlanId()),
                entry,
                ttlStrategy.planFeaturePolicyTtl(),
                "plan_feature_policy_write",
                PricingRedisOperationContext.plan(entry.pricingPlanId()));
    }

    public void invalidate(UUID planId) {
        redisAccessSupport.delete(
                PricingRedisKeys.planFeatures(planId),
                "plan_feature_policy_delete",
                PricingRedisOperationContext.plan(planId));
    }
}
