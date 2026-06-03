package com.lebhas.creativesaas.pricing.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.auditlog.application.AuditLogService;
import com.lebhas.creativesaas.auditlog.domain.AuditActionType;
import com.lebhas.creativesaas.auditlog.domain.AuditOutcome;
import com.lebhas.creativesaas.pricing.application.dto.PlanFeaturePolicyView;
import com.lebhas.creativesaas.pricing.application.dto.UpdatePlanFeaturePolicyCommand;
import com.lebhas.creativesaas.pricing.cache.PlanFeaturePolicyCacheService;
import com.lebhas.creativesaas.pricing.cache.PricingCacheInvalidationService;
import com.lebhas.creativesaas.pricing.cache.dto.PlanFeaturePolicyCacheEntry;
import com.lebhas.creativesaas.messaging.kafka.BaseDomainEvent;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.pricing.PlanFeaturePolicy;
import com.lebhas.pricing.PlanFeaturePolicyRepository;
import com.lebhas.pricing.WorkspaceSubscription;
import com.lebhas.pricing.WorkspaceSubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
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
    private DomainEventPublisher domainEventPublisher;
    private AuditLogService auditLogService;

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

    @Autowired(required = false)
    void setDomainEventPublisher(DomainEventPublisher domainEventPublisher) {
        this.domainEventPublisher = domainEventPublisher;
    }

    @Autowired(required = false)
    void setAuditLogService(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
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
                        command.maxAssets(),
                        command.maxCreativeRequests(),
                        command.maxTeamMembers(),
                        command.maxGeneratedVersionsPerCreativeRequest(),
                        command.maxStorageGb(),
                        command.maxStorageBytes(),
                        command.monthlyCreditLimit(),
                        command.promptEnhancementEnabled(),
                        command.creativeGenerationEnabled(),
                        command.allowApprovalWorkflow(),
                        command.downloadEnabled(),
                        command.shareEnabled(),
                        command.allowPublicShareLinks(),
                        command.assetUploadEnabled(),
                        command.premiumQualityEnabled(),
                        command.allowVideoGeneration(),
                        command.voiceoverGenerationEnabled(),
                        command.allowAdvancedPromptIntelligence(),
                        command.allowTeamCollaboration(),
                        command.allowExportWithoutWatermark(),
                        command.enabledCreativeToolCodes()));
        if (policy.getId() != null) {
            policy.update(
                    command.maxGeneratedVersionsPerRequest(),
                    command.maxBrands(),
                    command.maxProductServices(),
                    command.maxProjects(),
                    command.maxAssets(),
                    command.maxCreativeRequests(),
                    command.maxTeamMembers(),
                    command.maxGeneratedVersionsPerCreativeRequest(),
                    command.maxStorageGb(),
                    command.maxStorageBytes(),
                    command.monthlyCreditLimit(),
                    command.promptEnhancementEnabled(),
                    command.creativeGenerationEnabled(),
                    command.allowApprovalWorkflow(),
                    command.downloadEnabled(),
                    command.shareEnabled(),
                    command.allowPublicShareLinks(),
                    command.assetUploadEnabled(),
                    command.premiumQualityEnabled(),
                    command.allowVideoGeneration(),
                    command.voiceoverGenerationEnabled(),
                    command.allowAdvancedPromptIntelligence(),
                    command.allowTeamCollaboration(),
                    command.allowExportWithoutWatermark(),
                    command.enabledCreativeToolCodes());
        }
        policy = planFeaturePolicyRepository.save(policy);
        pricingCacheInvalidationService.invalidateFeaturePolicyUpdated(command.pricingPlanId());
        invalidateWorkspaceSubscriptionsForPlan(command.pricingPlanId());
        publishPolicyUpdated(command.pricingPlanId(), policy.getId());
        auditPolicyUpdated(command.pricingPlanId(), policy.getId());
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

    private void publishPolicyUpdated(UUID pricingPlanId, UUID policyId) {
        if (domainEventPublisher == null) {
            return;
        }
        domainEventPublisher.publish(KafkaTopicConstants.PLAN_FEATURE_POLICY_UPDATED, new BaseDomainEvent(
                KafkaTopicConstants.PLAN_FEATURE_POLICY_UPDATED,
                null,
                policyId,
                Instant.now(),
                Map.of(
                        "pricingPlanId", pricingPlanId.toString(),
                        "policyId", policyId.toString())));
    }

    private void auditPolicyUpdated(UUID pricingPlanId, UUID policyId) {
        if (auditLogService == null) {
            return;
        }
        auditLogService.appendCurrentUserAction(
                null,
                "plan.feature_policy.updated.%s".formatted(policyId),
                AuditActionType.UPDATE,
                AuditOutcome.SUCCESS,
                "PlanFeaturePolicy",
                policyId,
                "Plan feature policy updated",
                Map.of("pricingPlanId", pricingPlanId.toString(), "policyId", policyId.toString()),
                null,
                null);
    }
}
