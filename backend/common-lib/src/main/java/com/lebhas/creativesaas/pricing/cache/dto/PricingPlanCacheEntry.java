package com.lebhas.creativesaas.pricing.cache.dto;

import com.lebhas.pricing.PricingPlan;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PricingPlanCacheEntry(
        UUID id,
        String name,
        String code,
        String description,
        BigDecimal monthlyPrice,
        BigDecimal yearlyPrice,
        String currency,
        boolean defaultPlan,
        boolean active,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt,
        Instant cachedAt
) {

    public static PricingPlanCacheEntry from(PricingPlan pricingPlan) {
        return new PricingPlanCacheEntry(
                pricingPlan.getId(),
                pricingPlan.getName(),
                pricingPlan.getCode(),
                pricingPlan.getDescription(),
                pricingPlan.getMonthlyPrice(),
                pricingPlan.getYearlyPrice(),
                pricingPlan.getCurrency(),
                pricingPlan.isDefault(),
                pricingPlan.isActive(),
                pricingPlan.getSortOrder(),
                pricingPlan.getCreatedAt(),
                pricingPlan.getUpdatedAt(),
                Instant.now());
    }
}
