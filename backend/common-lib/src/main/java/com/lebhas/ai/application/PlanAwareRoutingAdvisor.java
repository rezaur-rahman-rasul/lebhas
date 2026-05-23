package com.lebhas.ai.application;

import com.lebhas.ai.application.dto.PlanRoutingPreference;
import com.lebhas.ai.application.dto.ProviderRoutingCandidate;
import com.lebhas.ai.application.dto.RoutingRecommendationType;
import com.lebhas.ai.application.dto.RoutingRecommendationView;
import com.lebhas.creativesaas.pricing.application.WorkspacePlanContextService;
import com.lebhas.creativesaas.pricing.application.dto.PlanFeaturePolicyView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspacePlanContextView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

@Service
public class PlanAwareRoutingAdvisor {

    private final WorkspacePlanContextService workspacePlanContextService;
    private final ProviderSelectionAdvisor providerSelectionAdvisor;

    public PlanAwareRoutingAdvisor(
            WorkspacePlanContextService workspacePlanContextService,
            ProviderSelectionAdvisor providerSelectionAdvisor
    ) {
        this.workspacePlanContextService = workspacePlanContextService;
        this.providerSelectionAdvisor = providerSelectionAdvisor;
    }

    @Transactional(readOnly = true)
    public PlanRoutingPreference preference(UUID workspaceId) {
        if (workspaceId == null) {
            return PlanRoutingPreference.BALANCED;
        }
        WorkspacePlanContextView context = workspacePlanContextService.getWorkspacePlanContext(workspaceId);
        PlanFeaturePolicyView policy = context.featurePolicy();
        if (policy == null) {
            return PlanRoutingPreference.BALANCED;
        }
        if (policy.allowAdvancedPromptIntelligence() || policy.allowVideoGeneration() || policy.allowExportWithoutWatermark()) {
            return PlanRoutingPreference.QUALITY_FOCUSED;
        }
        if (policy.monthlyCreditLimit() != null && policy.monthlyCreditLimit().signum() > 0) {
            return PlanRoutingPreference.BALANCED;
        }
        return PlanRoutingPreference.COST_EFFICIENT;
    }

    public RoutingRecommendationView recommendPlanAlignedProvider(
            UUID workspaceId,
            ProviderRoutingCandidate current,
            List<ProviderRoutingCandidate> candidates,
            Predicate<ProviderRoutingCandidate> eligibility
    ) {
        PlanRoutingPreference preference = preference(workspaceId);
        ProviderRoutingCandidate recommended = switch (preference) {
            case QUALITY_FOCUSED -> providerSelectionAdvisor.highestQuality(candidates, eligibility);
            case COST_EFFICIENT -> providerSelectionAdvisor.cheapest(candidates, eligibility);
            case BALANCED -> balancedCandidate(candidates, eligibility);
        };
        if (current == null || recommended == null || recommended.providerId().equals(current.providerId())) {
            return noRecommendation(current, "Current provider is already aligned with plan feature policy");
        }
        return new RoutingRecommendationView(
                RoutingRecommendationType.PLAN_QUALITY_ALIGNMENT,
                current.layerId(),
                current,
                recommended,
                savings(current, recommended),
                qualityGain(current, recommended),
                latencyImprovement(current, recommended),
                true,
                "Provider recommendation aligns with workspace plan feature policy");
    }

    private ProviderRoutingCandidate balancedCandidate(
            List<ProviderRoutingCandidate> candidates,
            Predicate<ProviderRoutingCandidate> eligibility
    ) {
        return candidates.stream()
                .filter(eligibility)
                .filter(candidate -> candidate.qualityToCostRatio() != null)
                .max((left, right) -> left.qualityToCostRatio().compareTo(right.qualityToCostRatio()))
                .orElseGet(() -> providerSelectionAdvisor.cheapest(candidates, eligibility));
    }

    private RoutingRecommendationView noRecommendation(ProviderRoutingCandidate current, String reason) {
        return new RoutingRecommendationView(RoutingRecommendationType.PLAN_QUALITY_ALIGNMENT, current == null ? null : current.layerId(),
                current, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false, reason);
    }

    private BigDecimal savings(ProviderRoutingCandidate current, ProviderRoutingCandidate recommended) {
        if (current.estimatedCostUsd() == null || recommended.estimatedCostUsd() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal savings = current.estimatedCostUsd().subtract(recommended.estimatedCostUsd());
        return savings.signum() < 0 ? BigDecimal.ZERO : savings;
    }

    private BigDecimal qualityGain(ProviderRoutingCandidate current, ProviderRoutingCandidate recommended) {
        if (current.qualityScore() == null || recommended.qualityScore() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal gain = recommended.qualityScore().subtract(current.qualityScore());
        return gain.signum() < 0 ? BigDecimal.ZERO : gain;
    }

    private BigDecimal latencyImprovement(ProviderRoutingCandidate current, ProviderRoutingCandidate recommended) {
        if (current.avgLatencyMs() == null || recommended.avgLatencyMs() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal improvement = current.avgLatencyMs().subtract(recommended.avgLatencyMs());
        return improvement.signum() < 0 ? BigDecimal.ZERO : improvement;
    }
}
