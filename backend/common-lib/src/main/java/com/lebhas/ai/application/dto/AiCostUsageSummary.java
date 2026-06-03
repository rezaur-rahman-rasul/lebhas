package com.lebhas.ai.application.dto;

import java.math.BigDecimal;

public record AiCostUsageSummary(
        BigDecimal totalCost,
        long totalRuns,
        BigDecimal averageCost
) {
}
