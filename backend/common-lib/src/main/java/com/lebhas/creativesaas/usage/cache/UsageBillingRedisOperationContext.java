package com.lebhas.creativesaas.usage.cache;

import java.time.LocalDate;
import java.util.UUID;

public record UsageBillingRedisOperationContext(
        UUID workspaceId,
        LocalDate month,
        UUID referenceId
) {
    public static UsageBillingRedisOperationContext of(UUID workspaceId, LocalDate month) {
        return new UsageBillingRedisOperationContext(workspaceId, month == null ? null : month.withDayOfMonth(1), null);
    }

    public static UsageBillingRedisOperationContext of(UUID workspaceId, LocalDate month, UUID referenceId) {
        return new UsageBillingRedisOperationContext(workspaceId, month == null ? null : month.withDayOfMonth(1), referenceId);
    }
}
