package com.lebhas.ai.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record LayerCostEstimate(
        UUID layerId,
        BigDecimal estimatedCostUsd,
        ProviderCostOption recommendedOption,
        List<ProviderCostOption> providerOptions
) {
    public LayerCostEstimate {
        providerOptions = providerOptions == null ? List.of() : List.copyOf(providerOptions);
    }
}
