package com.lebhas.creativesaas.pricing.cache;

import java.util.UUID;

public final class PricingRedisKeys {

    private static final String PRICING_PLAN = "pricing:plan:%s";
    private static final String ACTIVE_PLANS = "pricing:active-plans";
    private static final String WORKSPACE_SUBSCRIPTION = "workspace:subscription:%s";
    private static final String PLAN_FEATURES = "plan:features:%s";

    private PricingRedisKeys() {
    }

    public static String pricingPlan(UUID planId) {
        return PRICING_PLAN.formatted(require(planId, "planId"));
    }

    public static String activePlans() {
        return ACTIVE_PLANS;
    }

    public static String workspaceSubscription(UUID workspaceId) {
        return WORKSPACE_SUBSCRIPTION.formatted(require(workspaceId, "workspaceId"));
    }

    public static String planFeatures(UUID planId) {
        return PLAN_FEATURES.formatted(require(planId, "planId"));
    }

    private static UUID require(UUID value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }
}
