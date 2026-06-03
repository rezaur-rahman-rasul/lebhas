package com.lebhas.creativesaas.usage.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Backward-compatible factory name for older creative-service tests.
 */
@Deprecated(forRemoval = false)
public final class UsageBillingLogEntity {

    private UsageBillingLogEntity() {
    }

    public static UsageBillingLog create(
            UUID workspaceId,
            UUID generatedVersionId,
            String usageType,
            UUID referenceId,
            BigDecimal creditsCharged,
            String status
    ) {
        return UsageBillingLog.create(
                workspaceId,
                usageType,
                status,
                referenceId == null ? generatedVersionId : referenceId,
                creditsCharged,
                null,
                null,
                null);
    }
}
