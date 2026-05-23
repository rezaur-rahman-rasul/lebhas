package com.lebhas.creativesaas.usage.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "usage_billing_logs",
        schema = "platform",
        indexes = {
                @Index(name = "idx_usage_billing_logs_workspace_created_at", columnList = "workspace_id,created_at"),
                @Index(name = "idx_usage_billing_logs_usage_type_created_at", columnList = "usage_type,created_at"),
                @Index(name = "idx_usage_billing_logs_reference", columnList = "reference_type,reference_id"),
                @Index(name = "idx_usage_billing_logs_pricing_plan_id", columnList = "pricing_plan_id"),
                @Index(name = "idx_usage_billing_logs_plan_feature_policy_id", columnList = "plan_feature_policy_id")
        })
public class UsageBillingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    @Column(name = "usage_type", nullable = false, length = 80)
    private String usageType;

    @Column(name = "reference_type", length = 80)
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "credits_charged", nullable = false, precision = 19, scale = 4)
    private BigDecimal creditsCharged;

    @Column(name = "estimated_cost_usd", precision = 19, scale = 6)
    private BigDecimal estimatedCostUsd;

    @Column(name = "pricing_plan_id")
    private UUID pricingPlanId;

    @Column(name = "plan_feature_policy_id")
    private UUID planFeaturePolicyId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UsageBillingLog() {
    }

    public static UsageBillingLog create(
            UUID workspaceId,
            String usageType,
            String referenceType,
            UUID referenceId,
            BigDecimal creditsCharged,
            BigDecimal estimatedCostUsd,
            UUID pricingPlanId,
            UUID planFeaturePolicyId
    ) {
        UsageBillingLog log = new UsageBillingLog();
        log.workspaceId = require(workspaceId, "workspaceId");
        log.usageType = normalizeRequired(usageType, "usageType");
        log.referenceType = normalizeNullable(referenceType);
        log.referenceId = referenceId;
        log.creditsCharged = normalizeMoney(creditsCharged, "creditsCharged", 4);
        log.estimatedCostUsd = estimatedCostUsd == null ? null : normalizeMoney(estimatedCostUsd, "estimatedCostUsd", 6);
        log.pricingPlanId = pricingPlanId;
        log.planFeaturePolicyId = planFeaturePolicyId;
        return log;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public String getUsageType() {
        return usageType;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public BigDecimal getCreditsCharged() {
        return creditsCharged;
    }

    public BigDecimal getEstimatedCostUsd() {
        return estimatedCostUsd;
    }

    public UUID getPricingPlanId() {
        return pricingPlanId;
    }

    public UUID getPlanFeaturePolicyId() {
        return planFeaturePolicyId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static UUID require(UUID value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static BigDecimal normalizeMoney(BigDecimal value, String field, int scale) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value.setScale(scale, RoundingMode.HALF_UP);
    }
}
