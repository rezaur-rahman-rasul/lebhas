package com.lebhas.creativesaas.usage.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "monthly_usage_snapshots",
        schema = "platform",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_monthly_usage_snapshots_workspace_month",
                columnNames = {"workspace_id", "usage_month"}
        ),
        indexes = {
                @Index(name = "idx_monthly_usage_snapshots_workspace_id", columnList = "workspace_id"),
                @Index(name = "idx_monthly_usage_snapshots_usage_month", columnList = "usage_month"),
                @Index(name = "idx_monthly_usage_snapshots_pricing_plan_id", columnList = "pricing_plan_id"),
                @Index(name = "idx_monthly_usage_snapshots_subscription_id", columnList = "subscription_id")
        })
public class MonthlyUsageSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    @Column(name = "usage_month", nullable = false)
    private LocalDate usageMonth;

    @Column(name = "pricing_plan_id")
    private UUID pricingPlanId;

    @Column(name = "subscription_id")
    private UUID subscriptionId;

    @Column(name = "used_credits", nullable = false, precision = 19, scale = 4)
    private BigDecimal usedCredits;

    @Column(name = "generated_versions", nullable = false)
    private long generatedVersions;

    @Column(name = "creative_requests", nullable = false)
    private long creativeRequests;

    @Column(name = "ai_cost_usd", nullable = false, precision = 19, scale = 6)
    private BigDecimal aiCostUsd;

    @Column(name = "storage_bytes", nullable = false)
    private long storageBytes;

    @Column(name = "downloads", nullable = false)
    private long downloads;

    @Column(name = "public_shares", nullable = false)
    private long publicShares;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MonthlyUsageSnapshot() {
    }

    public static MonthlyUsageSnapshot create(
            UUID workspaceId,
            LocalDate usageMonth,
            UUID pricingPlanId,
            UUID subscriptionId,
            BigDecimal usedCredits,
            long generatedVersions,
            long creativeRequests,
            BigDecimal aiCostUsd,
            long storageBytes,
            long downloads,
            long publicShares
    ) {
        MonthlyUsageSnapshot snapshot = new MonthlyUsageSnapshot();
        snapshot.workspaceId = require(workspaceId, "workspaceId");
        snapshot.usageMonth = require(usageMonth, "usageMonth");
        snapshot.pricingPlanId = pricingPlanId;
        snapshot.subscriptionId = subscriptionId;
        snapshot.usedCredits = normalizeMoney(usedCredits, "usedCredits", 4);
        snapshot.generatedVersions = Math.max(generatedVersions, 0L);
        snapshot.creativeRequests = Math.max(creativeRequests, 0L);
        snapshot.aiCostUsd = normalizeMoney(aiCostUsd, "aiCostUsd", 6);
        snapshot.storageBytes = Math.max(storageBytes, 0L);
        snapshot.downloads = Math.max(downloads, 0L);
        snapshot.publicShares = Math.max(publicShares, 0L);
        return snapshot;
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

    public LocalDate getUsageMonth() {
        return usageMonth;
    }

    public UUID getPricingPlanId() {
        return pricingPlanId;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public BigDecimal getUsedCredits() {
        return usedCredits;
    }

    public long getGeneratedVersions() {
        return generatedVersions;
    }

    public long getCreativeRequests() {
        return creativeRequests;
    }

    public BigDecimal getAiCostUsd() {
        return aiCostUsd;
    }

    public long getStorageBytes() {
        return storageBytes;
    }

    public long getDownloads() {
        return downloads;
    }

    public long getPublicShares() {
        return publicShares;
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

    private static LocalDate require(LocalDate value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value.withDayOfMonth(1);
    }

    private static BigDecimal normalizeMoney(BigDecimal value, String field, int scale) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        BigDecimal normalized = value.setScale(scale, RoundingMode.HALF_UP);
        if (normalized.signum() < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        return normalized;
    }
}
