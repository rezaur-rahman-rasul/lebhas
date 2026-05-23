package com.lebhas.creativesaas.pricing.application.dto;

public record PricingPlanDetailView(
        PricingPlanView pricingPlan,
        PlanFeaturePolicyView featurePolicy
) {
}
