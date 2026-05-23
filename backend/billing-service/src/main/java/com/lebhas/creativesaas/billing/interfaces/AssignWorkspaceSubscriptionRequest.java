package com.lebhas.creativesaas.billing.interfaces;

import com.lebhas.pricing.WorkspaceSubscriptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Request payload to assign or change a workspace subscription.")
public record AssignWorkspaceSubscriptionRequest(
        @NotNull
        UUID pricingPlanId,
        WorkspaceSubscriptionStatus status,
        Instant startedAt,
        Instant expiresAt,
        Instant trialEndsAt,
        boolean autoRenew
) {

    @AssertTrue(message = "expiresAt must be on or after startedAt")
    public boolean isExpiryRangeValid() {
        return startedAt == null || expiresAt == null || !expiresAt.isBefore(startedAt);
    }

    @AssertTrue(message = "trialEndsAt must be on or after startedAt")
    public boolean isTrialRangeValid() {
        return startedAt == null || trialEndsAt == null || !trialEndsAt.isBefore(startedAt);
    }
}
