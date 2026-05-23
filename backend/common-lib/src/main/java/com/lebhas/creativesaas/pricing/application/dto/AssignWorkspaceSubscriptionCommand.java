package com.lebhas.creativesaas.pricing.application.dto;

import com.lebhas.pricing.WorkspaceSubscriptionStatus;

import java.time.Instant;
import java.util.UUID;

public record AssignWorkspaceSubscriptionCommand(
        UUID workspaceId,
        UUID pricingPlanId,
        WorkspaceSubscriptionStatus status,
        Instant startedAt,
        Instant expiresAt,
        Instant trialEndsAt,
        boolean autoRenew
) {
}
