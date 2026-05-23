package com.lebhas.creativesaas.usage.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record GenerationUsageBillingCommand(
        UUID workspaceId,
        UUID creativeRequestId,
        UUID generatedVersionId,
        BigDecimal estimatedCostUsd,
        BigDecimal creditsCharged,
        LocalDate usageMonth
) {
}
