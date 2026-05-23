package com.lebhas.creativesaas.pricing.cache.dto;

import java.time.Instant;
import java.util.List;

public record ActivePricingPlansCacheEntry(
        List<PricingPlanCacheEntry> plans,
        Instant cachedAt
) {

    public ActivePricingPlansCacheEntry {
        plans = plans == null ? List.of() : List.copyOf(plans);
    }
}
