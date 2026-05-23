package com.lebhas.creativesaas.usage.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AiCostUsageView(
        UUID usageBillingLogId,
        UUID workspaceId,
        UUID creativeRequestId,
        UUID generatedVersionId,
        UUID layerExecutionLogId,
        UUID layerId,
        UUID providerId,
        String providerCode,
        UUID modelId,
        String modelCode,
        LocalDate usageMonth,
        BigDecimal requestedUnits,
        BigDecimal estimatedCostUsd,
        BigDecimal creditsCharged,
        Instant createdAt
) {
}
