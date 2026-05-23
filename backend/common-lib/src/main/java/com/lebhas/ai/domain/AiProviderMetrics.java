package com.lebhas.ai.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_provider_metrics", schema = "platform")
public class AiProviderMetrics extends BaseEntity {

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "model_name", nullable = false, length = 160)
    private String modelName;

    @Column(name = "total_requests", nullable = false)
    private long totalRequests;

    @Column(name = "successful_requests", nullable = false)
    private long successfulRequests;

    @Column(name = "failed_requests", nullable = false)
    private long failedRequests;

    @Column(name = "avg_latency_ms", nullable = false, precision = 19, scale = 4)
    private BigDecimal avgLatencyMs = BigDecimal.ZERO;

    @Column(name = "avg_cost_usd", nullable = false, precision = 19, scale = 6)
    private BigDecimal avgCostUsd = BigDecimal.ZERO;

    @Column(name = "avg_quality_score", nullable = false, precision = 8, scale = 4)
    private BigDecimal avgQualityScore = BigDecimal.ZERO;

    @Column(name = "uptime_percentage", nullable = false, precision = 8, scale = 4)
    private BigDecimal uptimePercentage = BigDecimal.ZERO;

    @Column(name = "last_failure_at")
    private Instant lastFailureAt;

    @Column(name = "last_success_at")
    private Instant lastSuccessAt;

    protected AiProviderMetrics() {
    }

    public static AiProviderMetrics create(UUID providerId, String modelName) {
        AiProviderMetrics metrics = new AiProviderMetrics();
        metrics.providerId = AiToolProvider.require(providerId, "providerId");
        metrics.modelName = AiToolProvider.normalizeRequired(modelName, "modelName");
        return metrics;
    }

    public void updateTotals(
            long totalRequests,
            long successfulRequests,
            long failedRequests,
            BigDecimal avgLatencyMs,
            BigDecimal avgCostUsd,
            BigDecimal avgQualityScore,
            BigDecimal uptimePercentage,
            Instant lastFailureAt,
            Instant lastSuccessAt
    ) {
        this.totalRequests = nonNegative(totalRequests, "totalRequests");
        this.successfulRequests = nonNegative(successfulRequests, "successfulRequests");
        this.failedRequests = nonNegative(failedRequests, "failedRequests");
        this.avgLatencyMs = nonNegative(avgLatencyMs);
        this.avgCostUsd = nonNegative(avgCostUsd);
        this.avgQualityScore = nonNegative(avgQualityScore);
        this.uptimePercentage = nonNegative(uptimePercentage);
        this.lastFailureAt = lastFailureAt;
        this.lastSuccessAt = lastSuccessAt;
    }

    public UUID getProviderId() {
        return providerId;
    }

    public String getModelName() {
        return modelName;
    }

    public long getTotalRequests() {
        return totalRequests;
    }

    public long getSuccessfulRequests() {
        return successfulRequests;
    }

    public long getFailedRequests() {
        return failedRequests;
    }

    public BigDecimal getAvgLatencyMs() {
        return avgLatencyMs;
    }

    public BigDecimal getAvgCostUsd() {
        return avgCostUsd;
    }

    public BigDecimal getAvgQualityScore() {
        return avgQualityScore;
    }

    public BigDecimal getUptimePercentage() {
        return uptimePercentage;
    }

    public Instant getLastFailureAt() {
        return lastFailureAt;
    }

    public Instant getLastSuccessAt() {
        return lastSuccessAt;
    }

    static long nonNegative(long value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        return value;
    }

    static BigDecimal nonNegative(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value.signum() < 0) {
            throw new IllegalArgumentException("metric value must not be negative");
        }
        return value;
    }
}
