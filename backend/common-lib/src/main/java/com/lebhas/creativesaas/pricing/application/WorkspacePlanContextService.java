package com.lebhas.creativesaas.pricing.application;

import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.pricing.application.dto.PlanFeaturePolicyView;
import com.lebhas.creativesaas.pricing.application.dto.PricingPlanView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspacePlanContextView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspaceSubscriptionView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class WorkspacePlanContextService {

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final WorkspaceSubscriptionService workspaceSubscriptionService;
    private final PricingPlanQueryService pricingPlanQueryService;
    private final PlanFeaturePolicyService planFeaturePolicyService;
    private final PricingMapper pricingMapper;

    public WorkspacePlanContextService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            WorkspaceSubscriptionService workspaceSubscriptionService,
            PricingPlanQueryService pricingPlanQueryService,
            PlanFeaturePolicyService planFeaturePolicyService,
            PricingMapper pricingMapper
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.workspaceSubscriptionService = workspaceSubscriptionService;
        this.pricingPlanQueryService = pricingPlanQueryService;
        this.planFeaturePolicyService = planFeaturePolicyService;
        this.pricingMapper = pricingMapper;
    }

    @Transactional(readOnly = true)
    public WorkspacePlanContextView getWorkspacePlanContext(UUID workspaceId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService.requireWorkspaceContext(workspaceId);
        WorkspaceSubscriptionView subscription = workspaceSubscriptionService.findActiveWorkspaceSubscriptionView(access.workspace().getId())
                .orElse(null);
        if (subscription != null) {
            PricingPlanView pricingPlan = pricingPlanQueryService.findPricingPlanView(subscription.pricingPlanId()).orElse(null);
            PlanFeaturePolicyView featurePolicy = pricingPlan == null
                    ? null
                    : planFeaturePolicyService.findFeaturePolicyView(pricingPlan.id()).orElse(null);
            return pricingMapper.toWorkspacePlanContextView(
                    access.workspace().getId(),
                    subscription,
                    pricingPlan,
                    featurePolicy,
                    false);
        }

        PricingPlanView defaultPlan = pricingPlanQueryService.findDefaultActivePricingPlanView().orElse(null);
        PlanFeaturePolicyView featurePolicy = defaultPlan == null
                ? null
                : planFeaturePolicyService.findFeaturePolicyView(defaultPlan.id()).orElse(null);
        return pricingMapper.toWorkspacePlanContextView(
                access.workspace().getId(),
                null,
                defaultPlan,
                featurePolicy,
                defaultPlan != null);
    }
}
