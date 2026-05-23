package com.lebhas.creativesaas.creativerequest.cache.dto;

import com.lebhas.creativesaas.pricing.application.dto.PlanFeaturePolicyView;
import com.lebhas.creativesaas.pricing.application.dto.PricingPlanView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspacePlanContextView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspaceSubscriptionView;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record GenerationQuotaCacheEntry(
        UUID workspaceId,
        UUID subscriptionId,
        UUID pricingPlanId,
        String pricingPlanCode,
        Integer maxGeneratedVersionsPerRequest,
        BigDecimal monthlyCreditLimit,
        boolean allowApprovalWorkflow,
        boolean allowPublicShareLinks,
        boolean allowVideoGeneration,
        boolean allowAdvancedPromptIntelligence,
        boolean allowExportWithoutWatermark,
        Instant cachedAt
) {

    public static GenerationQuotaCacheEntry from(WorkspacePlanContextView planContext) {
        if (planContext == null) {
            return null;
        }
        WorkspaceSubscriptionView subscription = planContext.subscription();
        PricingPlanView pricingPlan = planContext.pricingPlan();
        PlanFeaturePolicyView featurePolicy = planContext.featurePolicy();
        return new GenerationQuotaCacheEntry(
                planContext.workspaceId(),
                subscription == null ? null : subscription.id(),
                pricingPlan == null ? null : pricingPlan.id(),
                pricingPlan == null ? null : pricingPlan.code(),
                featurePolicy == null ? null : featurePolicy.maxGeneratedVersionsPerRequest(),
                featurePolicy == null ? null : featurePolicy.monthlyCreditLimit(),
                featurePolicy != null && featurePolicy.allowApprovalWorkflow(),
                featurePolicy != null && featurePolicy.allowPublicShareLinks(),
                featurePolicy != null && featurePolicy.allowVideoGeneration(),
                featurePolicy != null && featurePolicy.allowAdvancedPromptIntelligence(),
                featurePolicy != null && featurePolicy.allowExportWithoutWatermark(),
                Instant.now());
    }
}
