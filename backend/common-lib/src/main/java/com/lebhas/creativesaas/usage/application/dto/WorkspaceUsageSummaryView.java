package com.lebhas.creativesaas.usage.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record WorkspaceUsageSummaryView(
        UUID id,
        UUID workspaceId,
        LocalDate usageMonth,
        BigDecimal usedCredits,
        BigDecimal reservedCredits,
        BigDecimal refundedCredits,
        long totalCreativeRequests,
        long totalGeneratedVersions,
        long totalLayerExecutions,
        BigDecimal totalAiCostUsd,
        long totalUploads,
        long totalStorageBytes,
        long totalDownloads,
        long totalPublicShares,
        long totalPromptEnhancements,
        long totalGenerationFailures,
        long totalApiCalls,
        Instant updatedAt
) {
}
