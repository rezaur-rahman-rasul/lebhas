package com.lebhas.creativesaas.pricing.cache.dto;

import com.lebhas.pricing.WorkspaceSubscription;
import com.lebhas.pricing.WorkspaceSubscriptionStatus;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceSubscriptionCacheEntry(
        UUID id,
        UUID workspaceId,
        UUID pricingPlanId,
        WorkspaceSubscriptionStatus status,
        Instant startedAt,
        Instant expiresAt,
        Instant trialEndsAt,
        boolean autoRenew,
        Instant createdAt,
        Instant updatedAt,
        Instant cachedAt
) {

    public static WorkspaceSubscriptionCacheEntry from(WorkspaceSubscription subscription) {
        return new WorkspaceSubscriptionCacheEntry(
                subscription.getId(),
                subscription.getWorkspaceId(),
                subscription.getPricingPlanId(),
                subscription.getStatus(),
                subscription.getStartedAt(),
                subscription.getExpiresAt(),
                subscription.getTrialEndsAt(),
                subscription.isAutoRenew(),
                subscription.getCreatedAt(),
                subscription.getUpdatedAt(),
                Instant.now());
    }
}
