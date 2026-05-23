package com.lebhas.creativesaas.billing.interfaces;

import com.lebhas.pricing.WorkspaceSubscriptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Workspace subscription response.")
public record WorkspaceSubscriptionResponse(
        UUID id,
        UUID workspaceId,
        UUID pricingPlanId,
        WorkspaceSubscriptionStatus status,
        Instant startedAt,
        Instant expiresAt,
        Instant trialEndsAt,
        boolean autoRenew,
        Instant createdAt,
        Instant updatedAt
) {
}
