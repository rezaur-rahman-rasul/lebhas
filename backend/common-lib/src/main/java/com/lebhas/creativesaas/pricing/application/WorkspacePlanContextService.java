package com.lebhas.creativesaas.pricing.application;

import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.pricing.application.dto.PlanFeaturePolicyView;
import com.lebhas.creativesaas.pricing.application.dto.PricingPlanView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspacePlanContextView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspaceSubscriptionView;
import com.lebhas.creativesaas.workspace.infrastructure.persistence.WorkspaceRepository;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
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
    private final WorkspaceRepository workspaceRepository;

    public WorkspacePlanContextService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            WorkspaceSubscriptionService workspaceSubscriptionService,
            PricingPlanQueryService pricingPlanQueryService,
            PlanFeaturePolicyService planFeaturePolicyService,
            PricingMapper pricingMapper,
            WorkspaceRepository workspaceRepository
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.workspaceSubscriptionService = workspaceSubscriptionService;
        this.pricingPlanQueryService = pricingPlanQueryService;
        this.planFeaturePolicyService = planFeaturePolicyService;
        this.pricingMapper = pricingMapper;
        this.workspaceRepository = workspaceRepository;
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

    @Transactional(readOnly = true)
    public WorkspacePlanContextView getWorkspacePlanContextForMaster(UUID workspaceId) {
        UUID existingWorkspaceId = workspaceRepository.findByIdAndDeletedFalse(workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND))
                .getId();
        WorkspaceSubscriptionView subscription = workspaceSubscriptionService.getWorkspaceSubscriptionForMaster(existingWorkspaceId)
                .orElse(null);
        if (subscription != null) {
            PricingPlanView pricingPlan = pricingPlanQueryService.findPricingPlanView(subscription.pricingPlanId()).orElse(null);
            PlanFeaturePolicyView featurePolicy = pricingPlan == null
                    ? null
                    : planFeaturePolicyService.getFeaturePolicyForMaster(pricingPlan.id()).orElse(null);
            return pricingMapper.toWorkspacePlanContextView(existingWorkspaceId, subscription, pricingPlan, featurePolicy, false);
        }
        PricingPlanView defaultPlan = pricingPlanQueryService.findDefaultActivePricingPlanView().orElse(null);
        PlanFeaturePolicyView featurePolicy = defaultPlan == null
                ? null
                : planFeaturePolicyService.getFeaturePolicyForMaster(defaultPlan.id()).orElse(null);
        return pricingMapper.toWorkspacePlanContextView(existingWorkspaceId, null, defaultPlan, featurePolicy, defaultPlan != null);
    }
}
