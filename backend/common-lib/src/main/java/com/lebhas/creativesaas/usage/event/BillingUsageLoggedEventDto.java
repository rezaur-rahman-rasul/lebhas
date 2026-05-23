package com.lebhas.creativesaas.usage.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BillingUsageLoggedEventDto(
        UUID workspaceId,
        UUID usageBillingLogId,
        String usageType,
        String referenceType,
        UUID referenceId,
        BigDecimal creditsCharged,
        BigDecimal estimatedCostUsd,
        UUID pricingPlanId,
        UUID planFeaturePolicyId,
        Instant occurredAt
) {
    public BillingUsageLoggedEventDto {
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}
