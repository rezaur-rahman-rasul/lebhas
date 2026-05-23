package com.lebhas.creativesaas.usage.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.pricing.PlanFeaturePolicy;
import com.lebhas.pricing.PlanFeaturePolicyRepository;
import com.lebhas.pricing.WorkspaceSubscription;
import com.lebhas.pricing.WorkspaceSubscriptionRepository;
import com.lebhas.pricing.WorkspaceSubscriptionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class PlanUsagePolicyResolver {

    private final WorkspaceSubscriptionRepository workspaceSubscriptionRepository;
    private final PlanFeaturePolicyRepository planFeaturePolicyRepository;

    public PlanUsagePolicyResolver(
            WorkspaceSubscriptionRepository workspaceSubscriptionRepository,
            PlanFeaturePolicyRepository planFeaturePolicyRepository
    ) {
        this.workspaceSubscriptionRepository = workspaceSubscriptionRepository;
        this.planFeaturePolicyRepository = planFeaturePolicyRepository;
    }

    @Transactional(readOnly = true)
    public PlanUsagePolicy resolve(UUID workspaceId) {
        WorkspaceSubscription subscription = workspaceSubscriptionRepository.findFirstByWorkspaceIdAndDeletedFalse(require(workspaceId, "workspaceId"))
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_SUBSCRIPTION_INACTIVE, "Workspace subscription is required"));
        if (!isActive(subscription)) {
            throw new BusinessException(ErrorCode.PLAN_SUBSCRIPTION_INACTIVE);
        }
        PlanFeaturePolicy policy = planFeaturePolicyRepository.findByPricingPlanIdAndDeletedFalse(subscription.getPricingPlanId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Plan feature policy is not configured"));
        return new PlanUsagePolicy(workspaceId, subscription, policy);
    }

    private boolean isActive(WorkspaceSubscription subscription) {
        WorkspaceSubscriptionStatus status = subscription.getStatus();
        boolean activeStatus = status == WorkspaceSubscriptionStatus.ACTIVE || status == WorkspaceSubscriptionStatus.TRIAL;
        if (!activeStatus) {
            return false;
        }
        Instant now = Instant.now();
        if (subscription.getExpiresAt() != null && subscription.getExpiresAt().isBefore(now)) {
            return false;
        }
        return subscription.getTrialEndsAt() == null || !subscription.getTrialEndsAt().isBefore(subscription.getStartedAt());
    }

    private UUID require(UUID value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    public record PlanUsagePolicy(
            UUID workspaceId,
            WorkspaceSubscription subscription,
            PlanFeaturePolicy featurePolicy
    ) {
    }
}
