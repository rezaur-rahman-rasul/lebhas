package com.lebhas.creativesaas.generation.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "creative_pipeline_runs", schema = "platform")
public class CreativePipelineRunEntity extends TenantAwareEntity {

    @Column(name = "creative_request_id", nullable = false)
    private UUID creativeRequestId;

    @Column(name = "primary_provider_code", nullable = false, length = 80)
    private String primaryProviderCode;

    @Column(name = "strategy", nullable = false, length = 80)
    private String strategy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CreativePipelineRunStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "plan_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> planJson = new LinkedHashMap<>();

    @Column(name = "estimated_credit_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal estimatedCreditCost;

    @Column(name = "actual_credit_cost", precision = 19, scale = 4)
    private BigDecimal actualCreditCost;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected CreativePipelineRunEntity() {
    }

    public static CreativePipelineRunEntity planned(
            UUID workspaceId,
            UUID creativeRequestId,
            String primaryProviderCode,
            String strategy,
            Map<String, Object> planJson,
            BigDecimal estimatedCreditCost
    ) {
        CreativePipelineRunEntity run = new CreativePipelineRunEntity();
        run.assignWorkspace(workspaceId);
        run.creativeRequestId = creativeRequestId;
        run.primaryProviderCode = required(primaryProviderCode, "primaryProviderCode");
        run.strategy = required(strategy, "strategy");
        run.status = CreativePipelineRunStatus.PLANNED;
        run.planJson = planJson == null ? new LinkedHashMap<>() : new LinkedHashMap<>(planJson);
        run.estimatedCreditCost = estimatedCreditCost == null ? BigDecimal.ZERO : estimatedCreditCost;
        return run;
    }

    public void markProcessing() {
        this.status = CreativePipelineRunStatus.PROCESSING;
    }

    public void markCompleted(BigDecimal actualCreditCost) {
        this.status = CreativePipelineRunStatus.COMPLETED;
        this.actualCreditCost = actualCreditCost;
        this.failureReason = null;
        this.completedAt = Instant.now();
    }

    public void markFailed(String failureReason) {
        this.status = CreativePipelineRunStatus.FAILED;
        this.failureReason = normalize(failureReason);
        this.completedAt = Instant.now();
    }

    public UUID getCreativeRequestId() { return creativeRequestId; }
    public String getPrimaryProviderCode() { return primaryProviderCode; }
    public String getStrategy() { return strategy; }
    public CreativePipelineRunStatus getStatus() { return status; }
    public Map<String, Object> getPlanJson() { return Map.copyOf(planJson); }
    public BigDecimal getEstimatedCreditCost() { return estimatedCreditCost; }
    public BigDecimal getActualCreditCost() { return actualCreditCost; }
    public String getFailureReason() { return failureReason; }
    public Instant getCompletedAt() { return completedAt; }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 1000 ? normalized : normalized.substring(0, 1000);
    }
}
