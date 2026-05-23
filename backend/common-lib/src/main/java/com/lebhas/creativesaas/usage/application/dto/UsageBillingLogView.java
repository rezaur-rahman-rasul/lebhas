package com.lebhas.creativesaas.usage.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record UsageBillingLogView(
        UUID id,
        UUID workspaceId,
        String usageType,
        String referenceType,
        UUID referenceId,
        BigDecimal creditsCharged,
        BigDecimal estimatedCostUsd,
        UUID pricingPlanId,
        UUID planFeaturePolicyId,
        Instant createdAt
) {
}
