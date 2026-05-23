package com.lebhas.ai.application;

import com.lebhas.ai.application.dto.ProviderRoutingCandidate;
import com.lebhas.ai.application.dto.RoutingRecommendationType;
import com.lebhas.ai.application.dto.RoutingRecommendationView;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Predicate;

@Service
public class CostQualityRoutingAdvisor {

    private final ProviderSelectionAdvisor providerSelectionAdvisor;

    public CostQualityRoutingAdvisor(ProviderSelectionAdvisor providerSelectionAdvisor) {
        this.providerSelectionAdvisor = providerSelectionAdvisor;
    }

    public RoutingRecommendationView recommendCheaperProvider(
            ProviderRoutingCandidate current,
            List<ProviderRoutingCandidate> candidates,
            Predicate<ProviderRoutingCandidate> eligibility
    ) {
        ProviderRoutingCandidate recommended = providerSelectionAdvisor.cheapest(candidates, eligibility);
        if (current == null || recommended == null || current.estimatedCostUsd() == null || recommended.estimatedCostUsd() == null) {
            return noRecommendation(RoutingRecommendationType.CHEAPER_PROVIDER, current, "No comparable provider cost data is available");
        }
        BigDecimal savings = current.estimatedCostUsd().subtract(recommended.estimatedCostUsd());
        if (savings.signum() <= 0 || recommended.providerId().equals(current.providerId())) {
            return noRecommendation(RoutingRecommendationType.CHEAPER_PROVIDER, current, "Current provider is already cost efficient");
        }
        return new RoutingRecommendationView(
                RoutingRecommendationType.CHEAPER_PROVIDER,
                current.layerId(),
                current,
                recommended,
                savings,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                true,
                "Lower estimated cost provider is available");
    }

    public RoutingRecommendationView recommendHigherQualityProvider(
            ProviderRoutingCandidate current,
            List<ProviderRoutingCandidate> candidates,
            Predicate<ProviderRoutingCandidate> eligibility
    ) {
        ProviderRoutingCandidate recommended = providerSelectionAdvisor.highestQuality(candidates, eligibility);
        if (current == null || recommended == null || current.qualityScore() == null || recommended.qualityScore() == null) {
            return noRecommendation(RoutingRecommendationType.HIGHER_QUALITY_PROVIDER, current, "No comparable provider quality data is available");
        }
        BigDecimal gain = recommended.qualityScore().subtract(current.qualityScore());
        if (gain.signum() <= 0 || recommended.providerId().equals(current.providerId())) {
            return noRecommendation(RoutingRecommendationType.HIGHER_QUALITY_PROVIDER, current, "Current provider is already quality competitive");
        }
        return new RoutingRecommendationView(
                RoutingRecommendationType.HIGHER_QUALITY_PROVIDER,
                current.layerId(),
                current,
                recommended,
                BigDecimal.ZERO,
                gain,
                BigDecimal.ZERO,
                true,
                "Higher observed quality provider is available");
    }

    public RoutingRecommendationView recommendFasterProvider(
            ProviderRoutingCandidate current,
            List<ProviderRoutingCandidate> candidates,
            Predicate<ProviderRoutingCandidate> eligibility
    ) {
        ProviderRoutingCandidate recommended = providerSelectionAdvisor.fastest(candidates, eligibility);
        if (current == null || recommended == null || current.avgLatencyMs() == null || recommended.avgLatencyMs() == null) {
            return noRecommendation(RoutingRecommendationType.FASTER_PROVIDER, current, "No comparable provider latency data is available");
        }
        BigDecimal improvement = current.avgLatencyMs().subtract(recommended.avgLatencyMs());
        if (improvement.signum() <= 0 || recommended.providerId().equals(current.providerId())) {
            return noRecommendation(RoutingRecommendationType.FASTER_PROVIDER, current, "Current provider is already speed competitive");
        }
        return new RoutingRecommendationView(
                RoutingRecommendationType.FASTER_PROVIDER,
                current.layerId(),
                current,
                recommended,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                improvement,
                true,
                "Lower observed latency provider is available");
    }

    private RoutingRecommendationView noRecommendation(
            RoutingRecommendationType type,
            ProviderRoutingCandidate current,
            String reason
    ) {
        return new RoutingRecommendationView(type, current == null ? null : current.layerId(), current, null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false, reason);
    }
}
