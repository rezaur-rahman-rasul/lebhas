package com.lebhas.creativesaas.pricing.cache;

import java.util.UUID;

public record PricingRedisOperationContext(
        UUID workspaceId,
        UUID pricingPlanId
) {

    public static PricingRedisOperationContext plan(UUID pricingPlanId) {
        return new PricingRedisOperationContext(null, pricingPlanId);
    }

    public static PricingRedisOperationContext workspace(UUID workspaceId) {
        return new PricingRedisOperationContext(workspaceId, null);
    }

    public static PricingRedisOperationContext workspacePlan(UUID workspaceId, UUID pricingPlanId) {
        return new PricingRedisOperationContext(workspaceId, pricingPlanId);
    }
}
