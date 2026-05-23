package com.lebhas.creativesaas.pricing.application.dto;

import java.util.UUID;

public record WorkspacePlanContextView(
        UUID workspaceId,
        WorkspaceSubscriptionView subscription,
        PricingPlanView pricingPlan,
        PlanFeaturePolicyView featurePolicy,
        boolean defaultPlanApplied
) {
}
