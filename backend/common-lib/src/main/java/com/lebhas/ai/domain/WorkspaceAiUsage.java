package com.lebhas.ai.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "workspace_ai_usage", schema = "platform")
public class WorkspaceAiUsage extends BaseEntity {

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "total_generation_requests", nullable = false)
    private long totalGenerationRequests;

    @Column(name = "total_generated_versions", nullable = false)
    private long totalGeneratedVersions;

    @Column(name = "total_credits_consumed", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalCreditsConsumed = BigDecimal.ZERO;

    @Column(name = "total_estimated_cost_usd", nullable = false, precision = 19, scale = 6)
    private BigDecimal totalEstimatedCostUsd = BigDecimal.ZERO;

    @Column(name = "total_failures", nullable = false)
    private long totalFailures;

    @Column(name = "avg_generation_time_ms", nullable = false, precision = 19, scale = 4)
    private BigDecimal avgGenerationTimeMs = BigDecimal.ZERO;

    protected WorkspaceAiUsage() {
    }

    public static WorkspaceAiUsage create(UUID workspaceId) {
        WorkspaceAiUsage usage = new WorkspaceAiUsage();
        usage.workspaceId = AiToolProvider.require(workspaceId, "workspaceId");
        return usage;
    }

    public void updateTotals(
            long totalGenerationRequests,
            long totalGeneratedVersions,
            BigDecimal totalCreditsConsumed,
            BigDecimal totalEstimatedCostUsd,
            long totalFailures,
            BigDecimal avgGenerationTimeMs
    ) {
        this.totalGenerationRequests = AiProviderMetrics.nonNegative(totalGenerationRequests, "totalGenerationRequests");
        this.totalGeneratedVersions = AiProviderMetrics.nonNegative(totalGeneratedVersions, "totalGeneratedVersions");
        this.totalCreditsConsumed = AiProviderMetrics.nonNegative(totalCreditsConsumed);
        this.totalEstimatedCostUsd = AiProviderMetrics.nonNegative(totalEstimatedCostUsd);
        this.totalFailures = AiProviderMetrics.nonNegative(totalFailures, "totalFailures");
        this.avgGenerationTimeMs = AiProviderMetrics.nonNegative(avgGenerationTimeMs);
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public long getTotalGenerationRequests() {
        return totalGenerationRequests;
    }

    public long getTotalGeneratedVersions() {
        return totalGeneratedVersions;
    }

    public BigDecimal getTotalCreditsConsumed() {
        return totalCreditsConsumed;
    }

    public BigDecimal getTotalEstimatedCostUsd() {
        return totalEstimatedCostUsd;
    }

    public long getTotalFailures() {
        return totalFailures;
    }

    public BigDecimal getAvgGenerationTimeMs() {
        return avgGenerationTimeMs;
    }
}
