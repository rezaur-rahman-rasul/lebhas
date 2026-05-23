package com.lebhas.creativesaas.billing.interfaces;

import com.lebhas.creativesaas.pricing.application.dto.PlanFeaturePolicyView;
import com.lebhas.creativesaas.pricing.application.dto.PricingPlanDetailView;
import com.lebhas.creativesaas.pricing.application.dto.PricingPlanView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspacePlanContextView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspaceSubscriptionView;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PricingApiMapper {

    public PricingPlanDetailResponse toPricingPlanDetailResponse(PricingPlanDetailView view) {
        return new PricingPlanDetailResponse(
                toPricingPlanResponse(view.pricingPlan()),
                toPlanFeaturePolicyResponse(view.featurePolicy()));
    }

    public List<PricingPlanDetailResponse> toPricingPlanDetailResponses(List<PricingPlanDetailView> views) {
        return views.stream().map(this::toPricingPlanDetailResponse).toList();
    }

    public PlanFeaturePolicyResponse toPlanFeaturePolicyResponse(PlanFeaturePolicyView view) {
        if (view == null) {
            return null;
        }
        return new PlanFeaturePolicyResponse(
                view.id(),
                view.pricingPlanId(),
                view.maxGeneratedVersionsPerRequest(),
                view.maxBrands(),
                view.maxProductServices(),
                view.maxProjects(),
                view.maxTeamMembers(),
                view.maxStorageGb(),
                view.monthlyCreditLimit(),
                view.allowApprovalWorkflow(),
                view.allowPublicShareLinks(),
                view.allowVideoGeneration(),
                view.allowAdvancedPromptIntelligence(),
                view.allowTeamCollaboration(),
                view.allowExportWithoutWatermark(),
                view.createdAt(),
                view.updatedAt());
    }

    public WorkspaceSubscriptionResponse toWorkspaceSubscriptionResponse(WorkspaceSubscriptionView view) {
        if (view == null) {
            return null;
        }
        return new WorkspaceSubscriptionResponse(
                view.id(),
                view.workspaceId(),
                view.pricingPlanId(),
                view.status(),
                view.startedAt(),
                view.expiresAt(),
                view.trialEndsAt(),
                view.autoRenew(),
                view.createdAt(),
                view.updatedAt());
    }

    public WorkspacePlanContextResponse toWorkspacePlanContextResponse(WorkspacePlanContextView view) {
        return new WorkspacePlanContextResponse(
                view.workspaceId(),
                toPricingPlanResponse(view.pricingPlan()),
                toWorkspaceSubscriptionResponse(view.subscription()),
                toPlanFeaturePolicyResponse(view.featurePolicy()),
                view.featurePolicy() == null ? null : view.featurePolicy().maxGeneratedVersionsPerRequest(),
                view.featurePolicy() == null ? null : view.featurePolicy().maxStorageGb(),
                view.featurePolicy() != null && view.featurePolicy().allowApprovalWorkflow(),
                view.featurePolicy() != null && view.featurePolicy().allowPublicShareLinks(),
                view.featurePolicy() == null ? null : view.featurePolicy().maxTeamMembers(),
                view.featurePolicy() == null ? null : view.featurePolicy().monthlyCreditLimit(),
                view.defaultPlanApplied());
    }

    private PricingPlanResponse toPricingPlanResponse(PricingPlanView view) {
        if (view == null) {
            return null;
        }
        return new PricingPlanResponse(
                view.id(),
                view.name(),
                view.code(),
                view.description(),
                view.monthlyPrice(),
                view.yearlyPrice(),
                view.currency(),
                view.defaultPlan(),
                view.active(),
                view.sortOrder(),
                view.createdAt(),
                view.updatedAt());
    }
}
