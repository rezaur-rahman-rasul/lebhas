package com.lebhas.creativesaas.creativerequest.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.generation.domain.CreativeType;
import com.lebhas.creativesaas.messaging.kafka.BaseDomainEvent;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.pricing.application.WorkspacePlanContextService;
import com.lebhas.creativesaas.pricing.application.dto.PlanFeaturePolicyView;
import com.lebhas.creativesaas.pricing.application.dto.PricingPlanView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspacePlanContextView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspaceSubscriptionView;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class CreativePlanValidationService {

    private static final String PLAN_LIMIT_EXCEEDED_EVENT = KafkaTopicConstants.PLAN_LIMIT_EXCEEDED;

    private final WorkspacePlanContextService workspacePlanContextService;
    private final ObjectProvider<DomainEventPublisher> domainEventPublisherProvider;

    public CreativePlanValidationService(
            WorkspacePlanContextService workspacePlanContextService,
            ObjectProvider<DomainEventPublisher> domainEventPublisherProvider
    ) {
        this.workspacePlanContextService = workspacePlanContextService;
        this.domainEventPublisherProvider = domainEventPublisherProvider;
    }

    public CreativePlanValidationResult validateForCreativeRequest(
            UUID workspaceId,
            int requestedVersions,
            CreativeType creativeType,
            BigDecimal estimatedCreditCost
    ) {
        WorkspacePlanContextView planContext = workspacePlanContextService.getWorkspacePlanContext(workspaceId);
        WorkspaceSubscriptionView subscription = planContext.subscription();
        if (subscription == null) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "An active workspace subscription is required before creating a creative request");
        }

        PricingPlanView pricingPlan = planContext.pricingPlan();
        if (pricingPlan == null || !pricingPlan.active()) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "The active workspace pricing plan is not available");
        }

        PlanFeaturePolicyView featurePolicy = planContext.featurePolicy();
        if (featurePolicy == null) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "The active workspace plan feature policy is not available");
        }

        validateRequestedVersions(workspaceId, pricingPlan, featurePolicy, requestedVersions);
        validateMonthlyCreditAvailability(featurePolicy, estimatedCreditCost);
        validateFeatureAvailability(featurePolicy, creativeType);

        return new CreativePlanValidationResult(subscription, pricingPlan, featurePolicy);
    }

    private void validateRequestedVersions(
            UUID workspaceId,
            PricingPlanView pricingPlan,
            PlanFeaturePolicyView featurePolicy,
            int requestedVersions
    ) {
        Integer maxGeneratedVersionsPerRequest = featurePolicy.maxGeneratedVersionsPerRequest();
        if (maxGeneratedVersionsPerRequest == null || maxGeneratedVersionsPerRequest < 1) {
            return;
        }
        if (requestedVersions > maxGeneratedVersionsPerRequest) {
            publishPlanLimitExceededSafely(
                    workspaceId,
                    pricingPlan,
                    Map.of(
                            "limitType", "maxGeneratedVersionsPerRequest",
                            "requestedVersions", requestedVersions,
                            "planLimit", maxGeneratedVersionsPerRequest));
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Requested versions exceed the current pricing plan limit");
        }
    }

    private void validateMonthlyCreditAvailability(PlanFeaturePolicyView featurePolicy, BigDecimal estimatedCreditCost) {
        if (featurePolicy.monthlyCreditLimit() == null || estimatedCreditCost == null) {
            return;
        }
        if (estimatedCreditCost.compareTo(featurePolicy.monthlyCreditLimit()) > 0) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Estimated credit usage exceeds the current pricing plan limit");
        }
    }

    private void validateFeatureAvailability(PlanFeaturePolicyView featurePolicy, CreativeType creativeType) {
        if (creativeType != null && creativeType.isVideo() && !featurePolicy.allowVideoGeneration()) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Video generation is not available for the current pricing plan");
        }
    }

    private void publishPlanLimitExceededSafely(UUID workspaceId, PricingPlanView pricingPlan, Map<String, Object> attributes) {
        DomainEventPublisher domainEventPublisher = domainEventPublisherProvider.getIfAvailable();
        if (domainEventPublisher == null) {
            return;
        }
        try {
            domainEventPublisher.publish(
                    PLAN_LIMIT_EXCEEDED_EVENT,
                    new BaseDomainEvent(
                            PLAN_LIMIT_EXCEEDED_EVENT,
                            workspaceId,
                            pricingPlan == null ? workspaceId : pricingPlan.id(),
                            Instant.now(),
                            attributes));
        } catch (RuntimeException ignored) {
        }
    }

    public record CreativePlanValidationResult(
            WorkspaceSubscriptionView subscription,
            PricingPlanView pricingPlan,
            PlanFeaturePolicyView featurePolicy
    ) {
    }
}
