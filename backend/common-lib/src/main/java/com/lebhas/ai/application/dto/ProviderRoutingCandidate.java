package com.lebhas.ai.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProviderRoutingCandidate(
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
        BigDecimal avgLatencyMs,
        BigDecimal reliabilityScore,
        BigDecimal failureRate,
        String healthStatus,
        long benchmarkSampleSize,
        long layerExecutionSampleSize,
        long recentFailureCount,
        boolean eligible,
        String ineligibilityReason
) {
}
