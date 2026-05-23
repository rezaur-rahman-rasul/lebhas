package com.lebhas.ai.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RoutingRecommendationView(
        RoutingRecommendationType type,
        UUID layerId,
        ProviderRoutingCandidate currentProvider,
        ProviderRoutingCandidate recommendedProvider,
        BigDecimal estimatedSavingsUsd,
        BigDecimal estimatedQualityGain,
        BigDecimal estimatedLatencyImprovementMs,
        boolean recommended,
        String reason
) {
}
