package com.lebhas.creativesaas.billing.interfaces;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Pricing plan detail response including feature policy.")
public record PricingPlanDetailResponse(
        PricingPlanResponse pricingPlan,
        PlanFeaturePolicyResponse featurePolicy
) {
}
