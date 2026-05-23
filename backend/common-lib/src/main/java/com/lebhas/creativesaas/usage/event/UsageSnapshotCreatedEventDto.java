package com.lebhas.creativesaas.usage.event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record UsageSnapshotCreatedEventDto(
        UUID workspaceId,
        UUID snapshotId,
        LocalDate usageMonth,
        UUID pricingPlanId,
        UUID subscriptionId,
        Instant occurredAt
) {
    public UsageSnapshotCreatedEventDto {
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}
