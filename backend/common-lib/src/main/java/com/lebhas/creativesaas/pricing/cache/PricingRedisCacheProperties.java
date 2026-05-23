package com.lebhas.creativesaas.pricing.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "platform.pricing.redis")
public class PricingRedisCacheProperties {

    private Duration pricingPlanTtl = Duration.ofMinutes(30);
    private Duration activePlansTtl = Duration.ofMinutes(10);
    private Duration planFeaturePolicyTtl = Duration.ofMinutes(30);
    private Duration workspaceSubscriptionTtl = Duration.ofMinutes(15);

    public Duration getPricingPlanTtl() {
        return pricingPlanTtl;
    }

    public void setPricingPlanTtl(Duration pricingPlanTtl) {
        this.pricingPlanTtl = pricingPlanTtl;
    }

    public Duration getActivePlansTtl() {
        return activePlansTtl;
    }

    public void setActivePlansTtl(Duration activePlansTtl) {
        this.activePlansTtl = activePlansTtl;
    }

    public Duration getPlanFeaturePolicyTtl() {
        return planFeaturePolicyTtl;
    }

    public void setPlanFeaturePolicyTtl(Duration planFeaturePolicyTtl) {
        this.planFeaturePolicyTtl = planFeaturePolicyTtl;
    }

    public Duration getWorkspaceSubscriptionTtl() {
        return workspaceSubscriptionTtl;
    }

    public void setWorkspaceSubscriptionTtl(Duration workspaceSubscriptionTtl) {
        this.workspaceSubscriptionTtl = workspaceSubscriptionTtl;
    }
}
