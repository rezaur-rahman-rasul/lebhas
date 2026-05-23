package com.lebhas.creativesaas.pricing.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.pricing.application.dto.PlanFeaturePolicyView;
import com.lebhas.creativesaas.pricing.application.dto.UpdatePlanFeaturePolicyCommand;
import com.lebhas.creativesaas.pricing.cache.PlanFeaturePolicyCacheService;
import com.lebhas.creativesaas.pricing.cache.PricingCacheInvalidationService;
import com.lebhas.creativesaas.pricing.cache.dto.PlanFeaturePolicyCacheEntry;
import com.lebhas.pricing.PlanFeaturePolicy;
import com.lebhas.pricing.PlanFeaturePolicyRepository;
import com.lebhas.pricing.WorkspaceSubscription;
import com.lebhas.pricing.WorkspaceSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class PlanFeaturePolicyService {

    private final CurrentUserContext currentUserContext;
    private final PricingPlanService pricingPlanService;
    private final PlanFeaturePolicyRepository planFeaturePolicyRepository;
    private final WorkspaceSubscriptionRepository workspaceSubscriptionRepository;
    private final PlanFeaturePolicyCacheService planFeaturePolicyCacheService;
    private final PricingCacheInvalidationService pricingCacheInvalidationService;
    private final PricingMapper pricingMapper;

    public PlanFeaturePolicyService(
            CurrentUserContext currentUserContext,
            PricingPlanService pricingPlanService,
            PlanFeaturePolicyRepository planFeaturePolicyRepository,
            WorkspaceSubscriptionRepository workspaceSubscriptionRepository,
            PlanFeaturePolicyCacheService planFeaturePolicyCacheService,
            PricingCacheInvalidationService pricingCacheInvalidationService,
            PricingMapper pricingMapper
    ) {
        this.currentUserContext = currentUserContext;
        this.pricingPlanService = pricingPlanService;
        this.planFeaturePolicyRepository = planFeaturePolicyRepository;
        this.workspaceSubscriptionRepository = workspaceSubscriptionRepository;
        this.planFeaturePolicyCacheService = planFeaturePolicyCacheService;
        this.pricingCacheInvalidationService = pricingCacheInvalidationService;
        this.pricingMapper = pricingMapper;
    }

    @Transactional
    public PlanFeaturePolicyView updateFeaturePolicy(UpdatePlanFeaturePolicyCommand command) {
        requireMaster();
        pricingPlanService.requirePricingPlan(command.pricingPlanId());
        PlanFeaturePolicy policy = planFeaturePolicyRepository.findByPricingPlanIdAndDeletedFalse(command.pricingPlanId())
                .orElseGet(() -> PlanFeaturePolicy.create(
                        command.pricingPlanId(),
                        command.maxGeneratedVersionsPerRequest(),
                        command.maxBrands(),
                        command.maxProductServices(),
                        command.maxProjects(),
                        command.maxTeamMembers(),
                        command.maxStorageGb(),
                        command.monthlyCreditLimit(),
                        command.allowApprovalWorkflow(),
                        command.allowPublicShareLinks(),
                        command.allowVideoGeneration(),
                        command.allowAdvancedPromptIntelligence(),
                        command.allowTeamCollaboration(),
                        command.allowExportWithoutWatermark()));
        if (policy.getId() != null) {
            policy.update(
                    command.maxGeneratedVersionsPerRequest(),
                    command.maxBrands(),
                    command.maxProductServices(),
                    command.maxProjects(),
                    command.maxTeamMembers(),
                    command.maxStorageGb(),
                    command.monthlyCreditLimit(),
                    command.allowApprovalWorkflow(),
                    command.allowPublicShareLinks(),
                    command.allowVideoGeneration(),
                    command.allowAdvancedPromptIntelligence(),
                    command.allowTeamCollaboration(),
                    command.allowExportWithoutWatermark());
        }
        policy = planFeaturePolicyRepository.save(policy);
        pricingCacheInvalidationService.invalidateFeaturePolicyUpdated(command.pricingPlanId());
        invalidateWorkspaceSubscriptionsForPlan(command.pricingPlanId());
        return pricingMapper.toPlanFeaturePolicyView(policy);
    }

    @Transactional(readOnly = true)
    public Optional<PlanFeaturePolicyView> getFeaturePolicyForMaster(UUID pricingPlanId) {
        requireMaster();
        pricingPlanService.requirePricingPlan(pricingPlanId);
        return findFeaturePolicyView(pricingPlanId);
    }

    @Transactional(readOnly = true)
    Optional<PlanFeaturePolicyView> findFeaturePolicyView(UUID pricingPlanId) {
        PlanFeaturePolicyCacheEntry cached = planFeaturePolicyCacheService.getOrLoad(
                        pricingPlanId,
                        () -> planFeaturePolicyRepository.findByPricingPlanIdAndDeletedFalse(pricingPlanId)
                                .map(PlanFeaturePolicyCacheEntry::from)
                                .orElse(null));
        if (cached == null) {
            return Optional.empty();
        }
        return Optional.of(pricingMapper.toPlanFeaturePolicyView(cached));
    }

    @Transactional(readOnly = true)
    PlanFeaturePolicy requireFeaturePolicy(UUID pricingPlanId) {
        return planFeaturePolicyRepository.findByPricingPlanIdAndDeletedFalse(pricingPlanId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Pricing plan feature policy not found"));
    }

    private CurrentUser requireMaster() {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        if (!currentUser.isMaster()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return currentUser;
    }

    private void invalidateWorkspaceSubscriptionsForPlan(UUID pricingPlanId) {
        pricingCacheInvalidationService.invalidateWorkspaceSubscriptions(
                workspaceSubscriptionRepository.findAllByPricingPlanIdAndDeletedFalse(pricingPlanId).stream()
                        .map(WorkspaceSubscription::getWorkspaceId)
                        .toList());
    }
}
