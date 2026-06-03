package com.lebhas.ai.application.dto;

import java.math.BigDecimal;

public record LayerAnalyticsSummary(
        long usageCount,
        BigDecimal averageCost,
        BigDecimal averageDurationMs,
        long failureCount
) {
}
