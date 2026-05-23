package com.lebhas.creativesaas.usage.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PlanUtilizationReportView(
        UUID workspaceId,
        LocalDate usageMonth,
        UUID pricingPlanId,
        UUID subscriptionId,
        BigDecimal usedCredits,
        BigDecimal monthlyCreditLimit,
        BigDecimal creditUtilizationPercent,
        long generatedVersions,
        Integer maxGeneratedVersionsPerRequest,
        long storageBytes,
        BigDecimal maxStorageGb,
        BigDecimal storageUtilizationPercent,
        long downloads,
        long publicShares
) {
}
