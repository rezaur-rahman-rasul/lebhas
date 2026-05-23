package com.lebhas.ai.application;

import com.lebhas.ai.application.dto.ProviderRoutingCandidate;
import com.lebhas.ai.application.dto.RoutingRecommendationType;
import com.lebhas.ai.application.dto.RoutingRecommendationView;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
public class FailureAwareRoutingAdvisor {

    public boolean isOperationallyEligible(ProviderRoutingCandidate candidate) {
        if (candidate == null || !candidate.eligible()) {
            return false;
        }
        return !"UNHEALTHY".equalsIgnoreCase(candidate.healthStatus());
    }

    public RoutingRecommendationView recommendFailureAvoidance(
            ProviderRoutingCandidate current,
            List<ProviderRoutingCandidate> candidates
    ) {
        if (current == null || isOperationallyEligible(current)) {
            return noRecommendation(RoutingRecommendationType.FAILURE_AVOIDANCE, current, "Current provider is not marked unhealthy");
        }
        ProviderRoutingCandidate replacement = candidates.stream()
                .filter(this::isOperationallyEligible)
                .min(Comparator.comparing(ProviderRoutingCandidate::failureRate)
                        .thenComparing(ProviderRoutingCandidate::recentFailureCount))
                .orElse(null);
        if (replacement == null) {
            return noRecommendation(RoutingRecommendationType.FAILURE_AVOIDANCE, current, "No healthier eligible provider is available");
        }
        return new RoutingRecommendationView(
                RoutingRecommendationType.FAILURE_AVOIDANCE,
                current.layerId(),
                current,
                replacement,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                true,
                "Health and recent failure data suggest avoiding the current provider");
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
