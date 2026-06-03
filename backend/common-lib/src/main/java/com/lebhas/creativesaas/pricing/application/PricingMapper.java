package com.lebhas.creativesaas.pricing.application;

import com.lebhas.creativesaas.pricing.application.dto.PlanFeaturePolicyView;
import com.lebhas.creativesaas.pricing.application.dto.PricingPlanDetailView;
import com.lebhas.creativesaas.pricing.application.dto.PricingPlanView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspacePlanContextView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspaceSubscriptionView;
import com.lebhas.creativesaas.pricing.cache.dto.PlanFeaturePolicyCacheEntry;
import com.lebhas.creativesaas.pricing.cache.dto.PricingPlanCacheEntry;
import com.lebhas.creativesaas.pricing.cache.dto.WorkspaceSubscriptionCacheEntry;
import com.lebhas.pricing.PlanFeaturePolicy;
import com.lebhas.pricing.PricingPlan;
import com.lebhas.pricing.WorkspaceSubscription;
import org.springframework.stereotype.Component;

@Component
public class PricingMapper {

    public PricingPlanView toPricingPlanView(PricingPlan pricingPlan) {
        return new PricingPlanView(
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
                pricingPlan.getUpdatedAt());
    }

    public PricingPlanView toPricingPlanView(PricingPlanCacheEntry pricingPlan) {
        return new PricingPlanView(
                pricingPlan.id(),
                pricingPlan.name(),
                pricingPlan.code(),
                pricingPlan.description(),
                pricingPlan.monthlyPrice(),
                pricingPlan.yearlyPrice(),
                pricingPlan.currency(),
                pricingPlan.defaultPlan(),
                pricingPlan.active(),
                pricingPlan.sortOrder(),
                pricingPlan.createdAt(),
                pricingPlan.updatedAt());
    }

    public PlanFeaturePolicyView toPlanFeaturePolicyView(PlanFeaturePolicy policy) {
        return new PlanFeaturePolicyView(
                policy.getId(),
                policy.getPricingPlanId(),
                policy.getMaxGeneratedVersionsPerRequest(),
                policy.getMaxBrands(),
                policy.getMaxProductServices(),
                policy.getMaxProjects(),
                policy.getMaxAssets(),
                policy.getMaxCreativeRequests(),
                policy.getMaxTeamMembers(),
                policy.getMaxGeneratedVersionsPerCreativeRequest(),
                policy.getMaxStorageGb(),
                policy.getMaxStorageBytes(),
                policy.getMonthlyCreditLimit(),
                policy.isPromptEnhancementEnabled(),
                policy.isCreativeGenerationEnabled(),
                policy.isAllowApprovalWorkflow(),
                policy.isDownloadEnabled(),
                policy.isShareEnabled(),
                policy.isAllowPublicShareLinks(),
                policy.isAssetUploadEnabled(),
                policy.isPremiumQualityEnabled(),
                policy.isAllowVideoGeneration(),
                policy.isVoiceoverGenerationEnabled(),
                policy.isAllowAdvancedPromptIntelligence(),
                policy.isAllowTeamCollaboration(),
                policy.isAllowExportWithoutWatermark(),
                policy.getEnabledCreativeToolCodes(),
                policy.getCreatedAt(),
                policy.getUpdatedAt());
    }

    public PlanFeaturePolicyView toPlanFeaturePolicyView(PlanFeaturePolicyCacheEntry policy) {
        return new PlanFeaturePolicyView(
                policy.id(),
                policy.pricingPlanId(),
                policy.maxGeneratedVersionsPerRequest(),
                policy.maxBrands(),
                policy.maxProductServices(),
                policy.maxProjects(),
                policy.maxAssets(),
                policy.maxCreativeRequests(),
                policy.maxTeamMembers(),
                policy.maxGeneratedVersionsPerCreativeRequest(),
                policy.maxStorageGb(),
                policy.maxStorageBytes(),
                policy.monthlyCreditLimit(),
                policy.promptEnhancementEnabled(),
                policy.creativeGenerationEnabled(),
                policy.allowApprovalWorkflow(),
                policy.downloadEnabled(),
                policy.shareEnabled(),
                policy.allowPublicShareLinks(),
                policy.assetUploadEnabled(),
                policy.premiumQualityEnabled(),
                policy.allowVideoGeneration(),
                policy.voiceoverGenerationEnabled(),
                policy.allowAdvancedPromptIntelligence(),
                policy.allowTeamCollaboration(),
                policy.allowExportWithoutWatermark(),
                policy.enabledCreativeToolCodes(),
                policy.createdAt(),
                policy.updatedAt());
    }

    public WorkspaceSubscriptionView toWorkspaceSubscriptionView(WorkspaceSubscription subscription) {
        return new WorkspaceSubscriptionView(
                subscription.getId(),
                subscription.getWorkspaceId(),
                subscription.getPricingPlanId(),
                subscription.getStatus(),
                subscription.getStartedAt(),
                subscription.getExpiresAt(),
                subscription.getTrialEndsAt(),
                subscription.isAutoRenew(),
                subscription.getCreatedAt(),
                subscription.getUpdatedAt());
    }

    public WorkspaceSubscriptionView toWorkspaceSubscriptionView(WorkspaceSubscriptionCacheEntry subscription) {
        return new WorkspaceSubscriptionView(
                subscription.id(),
                subscription.workspaceId(),
                subscription.pricingPlanId(),
                subscription.status(),
                subscription.startedAt(),
                subscription.expiresAt(),
                subscription.trialEndsAt(),
                subscription.autoRenew(),
                subscription.createdAt(),
                subscription.updatedAt());
    }

    public PricingPlanDetailView toPricingPlanDetailView(
            PricingPlanView pricingPlan,
            PlanFeaturePolicyView featurePolicy
    ) {
        return new PricingPlanDetailView(pricingPlan, featurePolicy);
    }

    public WorkspacePlanContextView toWorkspacePlanContextView(
            java.util.UUID workspaceId,
            WorkspaceSubscriptionView subscription,
            PricingPlanView pricingPlan,
            PlanFeaturePolicyView featurePolicy,
            boolean defaultPlanApplied
    ) {
        return new WorkspacePlanContextView(
                workspaceId,
                subscription,
                pricingPlan,
                featurePolicy,
                defaultPlanApplied);
    }
}
