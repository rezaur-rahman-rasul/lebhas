package com.lebhas.ai.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProviderCostOption(
        UUID layerId,
        UUID mappingId,
        UUID providerId,
        String providerCode,
        String providerName,
        UUID modelId,
        String modelCode,
        String modelName,
        BigDecimal estimatedCostUsd,
        BigDecimal qualityScore,
        BigDecimal qualityToCostRatio,
        boolean costKnown,
        boolean qualityKnown,
        boolean eligible,
        String ineligibilityReason
) {
}
