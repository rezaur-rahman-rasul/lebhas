package com.lebhas.ai.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProviderBenchmarkResult(
        UUID providerId,
        String providerCode,
        String providerName,
        String modelName,
        BigDecimal speedScore,
        BigDecimal costScore,
        BigDecimal qualityScore,
        BigDecimal reliabilityScore,
        BigDecimal overallScore,
        long sampleSize
) {
}
