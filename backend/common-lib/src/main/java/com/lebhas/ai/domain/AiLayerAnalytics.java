package com.lebhas.ai.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "ai_layer_analytics", schema = "platform")
public class AiLayerAnalytics extends BaseEntity {

    @Column(name = "layer_id", nullable = false)
    private UUID layerId;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "model_name", nullable = false, length = 160)
    private String modelName;

    @Column(name = "total_executions", nullable = false)
    private long totalExecutions;

    @Column(name = "successful_executions", nullable = false)
    private long successfulExecutions;

    @Column(name = "failed_executions", nullable = false)
    private long failedExecutions;

    @Column(name = "avg_execution_time_ms", nullable = false, precision = 19, scale = 4)
    private BigDecimal avgExecutionTimeMs = BigDecimal.ZERO;

    @Column(name = "avg_execution_cost_usd", nullable = false, precision = 19, scale = 6)
    private BigDecimal avgExecutionCostUsd = BigDecimal.ZERO;

    @Column(name = "avg_quality_score", nullable = false, precision = 8, scale = 4)
    private BigDecimal avgQualityScore = BigDecimal.ZERO;

    protected AiLayerAnalytics() {
    }

    public static AiLayerAnalytics create(UUID layerId, UUID providerId, String modelName) {
        AiLayerAnalytics analytics = new AiLayerAnalytics();
        analytics.layerId = AiToolProvider.require(layerId, "layerId");
        analytics.providerId = AiToolProvider.require(providerId, "providerId");
        analytics.modelName = AiToolProvider.normalizeRequired(modelName, "modelName");
        return analytics;
    }

    public void updateTotals(
            long totalExecutions,
            long successfulExecutions,
            long failedExecutions,
            BigDecimal avgExecutionTimeMs,
            BigDecimal avgExecutionCostUsd,
            BigDecimal avgQualityScore
    ) {
        this.totalExecutions = AiProviderMetrics.nonNegative(totalExecutions, "totalExecutions");
        this.successfulExecutions = AiProviderMetrics.nonNegative(successfulExecutions, "successfulExecutions");
        this.failedExecutions = AiProviderMetrics.nonNegative(failedExecutions, "failedExecutions");
        this.avgExecutionTimeMs = AiProviderMetrics.nonNegative(avgExecutionTimeMs);
        this.avgExecutionCostUsd = AiProviderMetrics.nonNegative(avgExecutionCostUsd);
        this.avgQualityScore = AiProviderMetrics.nonNegative(avgQualityScore);
    }

    public UUID getLayerId() {
        return layerId;
    }

    public UUID getProviderId() {
        return providerId;
    }

    public String getModelName() {
        return modelName;
    }

    public long getTotalExecutions() {
        return totalExecutions;
    }

    public long getSuccessfulExecutions() {
        return successfulExecutions;
    }

    public long getFailedExecutions() {
        return failedExecutions;
    }

    public BigDecimal getAvgExecutionTimeMs() {
        return avgExecutionTimeMs;
    }

    public BigDecimal getAvgExecutionCostUsd() {
        return avgExecutionCostUsd;
    }

    public BigDecimal getAvgQualityScore() {
        return avgQualityScore;
    }
}
