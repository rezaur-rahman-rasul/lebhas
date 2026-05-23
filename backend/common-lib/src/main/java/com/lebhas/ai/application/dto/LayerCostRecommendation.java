package com.lebhas.ai.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record LayerCostRecommendation(
        UUID layerId,
        ProviderCostOption currentOption,
        ProviderCostOption recommendedOption,
        BigDecimal estimatedSavingsUsd,
        boolean routingChangeRecommended,
        String reason
) {
}
