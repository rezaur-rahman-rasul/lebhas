package com.lebhas.creativesaas.pricing.cache;

import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class PricingRedisTtlStrategy {

    private final PricingRedisCacheProperties properties;

    public PricingRedisTtlStrategy(PricingRedisCacheProperties properties) {
        this.properties = properties;
    }

    public Duration pricingPlanTtl() {
        return normalize(properties.getPricingPlanTtl(), Duration.ofMinutes(30));
    }

    public Duration activePlansTtl() {
        return normalize(properties.getActivePlansTtl(), Duration.ofMinutes(10));
    }

    public Duration planFeaturePolicyTtl() {
        return normalize(properties.getPlanFeaturePolicyTtl(), Duration.ofMinutes(30));
    }

    public Duration workspaceSubscriptionTtl() {
        return normalize(properties.getWorkspaceSubscriptionTtl(), Duration.ofMinutes(15));
    }

    private Duration normalize(Duration candidate, Duration fallback) {
        if (candidate == null || candidate.isNegative() || candidate.isZero()) {
            return fallback;
        }
        return candidate;
    }
}
