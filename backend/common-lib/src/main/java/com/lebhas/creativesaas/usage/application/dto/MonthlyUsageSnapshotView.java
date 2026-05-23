package com.lebhas.creativesaas.usage.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MonthlyUsageSnapshotView(
        UUID id,
        UUID workspaceId,
        LocalDate usageMonth,
        UUID pricingPlanId,
        UUID subscriptionId,
        BigDecimal usedCredits,
        long generatedVersions,
        long creativeRequests,
        BigDecimal aiCostUsd,
        long storageBytes,
        long downloads,
        long publicShares,
        Instant createdAt
) {
}
