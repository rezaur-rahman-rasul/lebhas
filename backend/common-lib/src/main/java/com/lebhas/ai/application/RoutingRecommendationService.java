package com.lebhas.ai.application;

import com.lebhas.ai.application.dto.DynamicRoutingOptimizationResult;
import com.lebhas.ai.application.dto.PlanRoutingPreference;
import com.lebhas.ai.application.dto.ProviderRoutingCandidate;
import com.lebhas.ai.application.dto.RoutingOptimizationRequest;
import com.lebhas.ai.application.dto.RoutingRecommendationView;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Service
public class RoutingRecommendationService {

    private final ProviderSelectionAdvisor providerSelectionAdvisor;
    private final CostQualityRoutingAdvisor costQualityRoutingAdvisor;
    private final FailureAwareRoutingAdvisor failureAwareRoutingAdvisor;
    private final PlanAwareRoutingAdvisor planAwareRoutingAdvisor;

    public RoutingRecommendationService(
            ProviderSelectionAdvisor providerSelectionAdvisor,
            CostQualityRoutingAdvisor costQualityRoutingAdvisor,
            FailureAwareRoutingAdvisor failureAwareRoutingAdvisor,
            PlanAwareRoutingAdvisor planAwareRoutingAdvisor
    ) {
        this.providerSelectionAdvisor = providerSelectionAdvisor;
        this.costQualityRoutingAdvisor = costQualityRoutingAdvisor;
        this.failureAwareRoutingAdvisor = failureAwareRoutingAdvisor;
        this.planAwareRoutingAdvisor = planAwareRoutingAdvisor;
    }

    public DynamicRoutingOptimizationResult recommend(RoutingOptimizationRequest request) {
        List<ProviderRoutingCandidate> candidates = providerSelectionAdvisor.candidates(request.layerId(), request.toCostEstimateInput());
        ProviderRoutingCandidate current = providerSelectionAdvisor.currentProvider(request.layerId(), request.toCostEstimateInput());
        Predicate<ProviderRoutingCandidate> eligibility = failureAwareRoutingAdvisor::isOperationallyEligible;
        List<RoutingRecommendationView> recommendations = new ArrayList<>();
        recommendations.add(costQualityRoutingAdvisor.recommendCheaperProvider(current, candidates, eligibility));
        recommendations.add(costQualityRoutingAdvisor.recommendHigherQualityProvider(current, candidates, eligibility));
        recommendations.add(costQualityRoutingAdvisor.recommendFasterProvider(current, candidates, eligibility));
        recommendations.add(failureAwareRoutingAdvisor.recommendFailureAvoidance(current, candidates));
        recommendations.add(planAwareRoutingAdvisor.recommendPlanAlignedProvider(request.workspaceId(), current, candidates, eligibility));
        PlanRoutingPreference preference = planAwareRoutingAdvisor.preference(request.workspaceId());
        return new DynamicRoutingOptimizationResult(
                request.workspaceId(),
                request.layerId(),
                preference,
                current,
                candidates,
                recommendations);
    }
}
