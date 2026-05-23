package com.lebhas.pricing;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workspace_subscriptions", schema = "platform")
public class WorkspaceSubscription extends TenantAwareEntity {

    @Column(name = "pricing_plan_id", nullable = false)
    private UUID pricingPlanId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private WorkspaceSubscriptionStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "trial_ends_at")
    private Instant trialEndsAt;

    @Column(name = "auto_renew", nullable = false)
    private boolean autoRenew;

    protected WorkspaceSubscription() {
    }

    public static WorkspaceSubscription create(
            UUID workspaceId,
            UUID pricingPlanId,
            WorkspaceSubscriptionStatus status,
            Instant startedAt,
            Instant expiresAt,
            Instant trialEndsAt,
            boolean autoRenew
    ) {
        WorkspaceSubscription subscription = new WorkspaceSubscription();
        subscription.assignWorkspace(requireWorkspaceId(workspaceId));
        subscription.pricingPlanId = requirePricingPlanId(pricingPlanId);
        subscription.status = requireStatus(status);
        subscription.startedAt = requireStartedAt(startedAt);
        subscription.expiresAt = expiresAt;
        subscription.trialEndsAt = trialEndsAt;
        subscription.autoRenew = autoRenew;
        subscription.validateDates();
        return subscription;
    }

    public UUID getPricingPlanId() {
        return pricingPlanId;
    }

    public WorkspaceSubscriptionStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getTrialEndsAt() {
        return trialEndsAt;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }

    public void update(
            UUID pricingPlanId,
            WorkspaceSubscriptionStatus status,
            Instant startedAt,
            Instant expiresAt,
            Instant trialEndsAt,
            boolean autoRenew
    ) {
        this.pricingPlanId = requirePricingPlanId(pricingPlanId);
        this.status = requireStatus(status);
        this.startedAt = requireStartedAt(startedAt);
        this.expiresAt = expiresAt;
        this.trialEndsAt = trialEndsAt;
        this.autoRenew = autoRenew;
        validateDates();
    }

    public void changePlan(UUID pricingPlanId) {
        this.pricingPlanId = requirePricingPlanId(pricingPlanId);
    }

    public void changeStatus(WorkspaceSubscriptionStatus status) {
        this.status = requireStatus(status);
    }

    public void updateDates(Instant startedAt, Instant expiresAt, Instant trialEndsAt) {
        this.startedAt = requireStartedAt(startedAt);
        this.expiresAt = expiresAt;
        this.trialEndsAt = trialEndsAt;
        validateDates();
    }

    public void setAutoRenew(boolean autoRenew) {
        this.autoRenew = autoRenew;
    }

    private void validateDates() {
        if (expiresAt != null && expiresAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("expiresAt must be on or after startedAt");
        }
        if (trialEndsAt != null && trialEndsAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("trialEndsAt must be on or after startedAt");
        }
    }

    private static UUID requireWorkspaceId(UUID workspaceId) {
        if (workspaceId == null) {
            throw new IllegalArgumentException("workspaceId must not be null");
        }
        return workspaceId;
    }

    private static UUID requirePricingPlanId(UUID pricingPlanId) {
        if (pricingPlanId == null) {
            throw new IllegalArgumentException("pricingPlanId must not be null");
        }
        return pricingPlanId;
    }

    private static WorkspaceSubscriptionStatus requireStatus(WorkspaceSubscriptionStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        return status;
    }

    private static Instant requireStartedAt(Instant startedAt) {
        if (startedAt == null) {
            throw new IllegalArgumentException("startedAt must not be null");
        }
        return startedAt;
    }
}
