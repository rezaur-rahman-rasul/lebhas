package com.lebhas.creativesaas.usage.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "workspace_usage_summaries",
        schema = "platform",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_workspace_usage_summaries_workspace_month",
                columnNames = {"workspace_id", "usage_month"}
        ),
        indexes = {
                @Index(name = "idx_workspace_usage_summaries_workspace_id", columnList = "workspace_id"),
                @Index(name = "idx_workspace_usage_summaries_usage_month", columnList = "usage_month")
        })
public class WorkspaceUsageSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    @Column(name = "usage_month", nullable = false)
    private LocalDate usageMonth;

    @Column(name = "used_credits", nullable = false, precision = 19, scale = 4)
    private BigDecimal usedCredits;

    @Column(name = "reserved_credits", nullable = false, precision = 19, scale = 4)
    private BigDecimal reservedCredits;

    @Column(name = "refunded_credits", nullable = false, precision = 19, scale = 4)
    private BigDecimal refundedCredits;

    @Column(name = "total_creative_requests", nullable = false)
    private long totalCreativeRequests;

    @Column(name = "total_generated_versions", nullable = false)
    private long totalGeneratedVersions;

    @Column(name = "total_layer_executions", nullable = false)
    private long totalLayerExecutions;

    @Column(name = "total_ai_cost_usd", nullable = false, precision = 19, scale = 6)
    private BigDecimal totalAiCostUsd;

    @Column(name = "total_uploads", nullable = false)
    private long totalUploads;

    @Column(name = "total_storage_bytes", nullable = false)
    private long totalStorageBytes;

    @Column(name = "total_downloads", nullable = false)
    private long totalDownloads;

    @Column(name = "total_public_shares", nullable = false)
    private long totalPublicShares;

    @Column(name = "total_prompt_enhancements", nullable = false)
    private long totalPromptEnhancements;

    @Column(name = "total_generation_failures", nullable = false)
    private long totalGenerationFailures;

    @Column(name = "total_api_calls", nullable = false)
    private long totalApiCalls;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WorkspaceUsageSummary() {
    }

    public static WorkspaceUsageSummary create(UUID workspaceId, LocalDate usageMonth) {
        WorkspaceUsageSummary summary = new WorkspaceUsageSummary();
        summary.workspaceId = require(workspaceId, "workspaceId");
        summary.usageMonth = require(usageMonth, "usageMonth");
        summary.usedCredits = zero(4);
        summary.reservedCredits = zero(4);
        summary.refundedCredits = zero(4);
        summary.totalAiCostUsd = zero(6);
        return summary;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
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

    public BigDecimal getUsedCredits() {
        return usedCredits;
    }

    public BigDecimal getReservedCredits() {
        return reservedCredits;
    }

    public BigDecimal getRefundedCredits() {
        return refundedCredits;
    }

    public long getTotalCreativeRequests() {
        return totalCreativeRequests;
    }

    public long getTotalGeneratedVersions() {
        return totalGeneratedVersions;
    }

    public long getTotalLayerExecutions() {
        return totalLayerExecutions;
    }

    public BigDecimal getTotalAiCostUsd() {
        return totalAiCostUsd;
    }

    public long getTotalUploads() {
        return totalUploads;
    }

    public long getTotalStorageBytes() {
        return totalStorageBytes;
    }

    public long getTotalDownloads() {
        return totalDownloads;
    }

    public long getTotalPublicShares() {
        return totalPublicShares;
    }

    public long getTotalPromptEnhancements() {
        return totalPromptEnhancements;
    }

    public long getTotalGenerationFailures() {
        return totalGenerationFailures;
    }

    public long getTotalApiCalls() {
        return totalApiCalls;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void recordReservation(BigDecimal credits) {
        this.reservedCredits = normalizeNonNegative(reservedCredits.add(normalizeCredits(credits)), "reservedCredits");
        this.totalCreativeRequests += 1;
        this.totalApiCalls += 1;
    }

    public void recordFinalization(BigDecimal credits) {
        BigDecimal normalizedCredits = normalizeCredits(credits);
        this.reservedCredits = normalizeNonNegative(reservedCredits.subtract(normalizedCredits), "reservedCredits");
        this.usedCredits = normalizeNonNegative(usedCredits.add(normalizedCredits), "usedCredits");
        this.totalGeneratedVersions += 1;
        this.totalApiCalls += 1;
    }

    public void recordRefund(BigDecimal credits) {
        BigDecimal normalizedCredits = normalizeCredits(credits);
        this.reservedCredits = normalizeNonNegative(reservedCredits.subtract(normalizedCredits), "reservedCredits");
        this.refundedCredits = normalizeNonNegative(refundedCredits.add(normalizedCredits), "refundedCredits");
        this.totalGenerationFailures += 1;
        this.totalApiCalls += 1;
    }

    public void recordStorageBytes(long totalStorageBytes) {
        this.totalStorageBytes = Math.max(totalStorageBytes, 0L);
    }

    public void recordLayerExecutionCost(BigDecimal estimatedCostUsd) {
        this.totalLayerExecutions += 1;
        this.totalAiCostUsd = normalizeMoney(
                totalAiCostUsd.add(normalizeMoney(estimatedCostUsd, "estimatedCostUsd", 6)),
                "totalAiCostUsd",
                6);
        this.totalApiCalls += 1;
    }

    public void recordDownload() {
        this.totalDownloads += 1;
        this.totalApiCalls += 1;
    }

    public void recordPublicShareAccess() {
        this.totalPublicShares += 1;
        this.totalApiCalls += 1;
    }

    public void recordGenerationCompletedUsage(BigDecimal credits) {
        this.usedCredits = normalizeNonNegative(usedCredits.add(normalizeCredits(credits)), "usedCredits");
        this.totalGeneratedVersions += 1;
        this.totalApiCalls += 1;
    }

    public void recordGenerationFailureUsage() {
        this.totalGenerationFailures += 1;
        this.totalApiCalls += 1;
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

    private static BigDecimal zero(int scale) {
        return BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
    }

    private static BigDecimal normalizeCredits(BigDecimal value) {
        BigDecimal normalized = value == null ? zero(4) : value.setScale(4, RoundingMode.HALF_UP);
        if (normalized.signum() < 0) {
            throw new IllegalArgumentException("credits must not be negative");
        }
        return normalized;
    }

    private static BigDecimal normalizeNonNegative(BigDecimal value, String field) {
        BigDecimal normalized = value.setScale(4, RoundingMode.HALF_UP);
        if (normalized.signum() < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        return normalized;
    }

    private static BigDecimal normalizeMoney(BigDecimal value, String field, int scale) {
        BigDecimal normalized = value == null ? zero(scale) : value.setScale(scale, RoundingMode.HALF_UP);
        if (normalized.signum() < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        return normalized;
    }
}
