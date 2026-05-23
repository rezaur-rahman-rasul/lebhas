package com.lebhas.creativesaas.pricing.cache;

import com.lebhas.creativesaas.pricing.cache.dto.ActivePricingPlansCacheEntry;
import com.lebhas.creativesaas.pricing.cache.dto.PricingPlanCacheEntry;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class PricingPlanCacheService {

    private final PricingRedisAccessSupport redisAccessSupport;
    private final PricingRedisTtlStrategy ttlStrategy;

    public PricingPlanCacheService(
            PricingRedisAccessSupport redisAccessSupport,
            PricingRedisTtlStrategy ttlStrategy
    ) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<PricingPlanCacheEntry> get(UUID planId) {
        return redisAccessSupport.read(
                PricingRedisKeys.pricingPlan(planId),
                PricingPlanCacheEntry.class,
                "pricing_plan_read",
                PricingRedisOperationContext.plan(planId));
    }

    public PricingPlanCacheEntry getOrLoad(UUID planId, Supplier<PricingPlanCacheEntry> loader) {
        return get(planId).orElseGet(() -> {
            PricingPlanCacheEntry loaded = loader.get();
            if (loaded != null) {
                store(loaded);
            }
            return loaded;
        });
    }

    public void store(PricingPlanCacheEntry entry) {
        if (entry == null || entry.id() == null) {
            return;
        }
        redisAccessSupport.write(
                PricingRedisKeys.pricingPlan(entry.id()),
                entry,
                ttlStrategy.pricingPlanTtl(),
                "pricing_plan_write",
                PricingRedisOperationContext.plan(entry.id()));
    }

    public void invalidate(UUID planId) {
        redisAccessSupport.delete(
                PricingRedisKeys.pricingPlan(planId),
                "pricing_plan_delete",
                PricingRedisOperationContext.plan(planId));
    }

    public Optional<ActivePricingPlansCacheEntry> getActivePlans() {
        return redisAccessSupport.read(
                PricingRedisKeys.activePlans(),
                ActivePricingPlansCacheEntry.class,
                "pricing_active_plans_read",
                null);
    }

    public ActivePricingPlansCacheEntry getActivePlansOrLoad(Supplier<ActivePricingPlansCacheEntry> loader) {
        return getActivePlans().orElseGet(() -> {
            ActivePricingPlansCacheEntry loaded = loader.get();
            if (loaded != null) {
                storeActivePlans(loaded);
            }
            return loaded;
        });
    }

    public void storeActivePlans(ActivePricingPlansCacheEntry entry) {
        if (entry == null) {
            return;
        }
        redisAccessSupport.write(
                PricingRedisKeys.activePlans(),
                entry,
                ttlStrategy.activePlansTtl(),
                "pricing_active_plans_write",
                null);
    }

    public void invalidateActivePlans() {
        redisAccessSupport.delete(
                PricingRedisKeys.activePlans(),
                "pricing_active_plans_delete",
                null);
    }
}
