package com.lebhas.ai.application.dto;

import java.util.List;
import java.util.UUID;

public record DynamicRoutingOptimizationResult(
        UUID workspaceId,
        UUID layerId,
        PlanRoutingPreference planRoutingPreference,
        ProviderRoutingCandidate currentProvider,
        List<ProviderRoutingCandidate> candidates,
        List<RoutingRecommendationView> recommendations
) {
    public DynamicRoutingOptimizationResult {
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
    }
}
