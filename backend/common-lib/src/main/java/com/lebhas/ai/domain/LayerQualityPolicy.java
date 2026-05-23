package com.lebhas.ai.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "layer_quality_policies", schema = "platform")
public class LayerQualityPolicy extends BaseEntity {

    @Column(name = "pipeline_layer_id", nullable = false)
    private UUID pipelineLayerId;

    @Column(name = "policy_code", nullable = false, length = 120)
    private String policyCode;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "priority_order", nullable = false)
    private int priorityOrder;

    @Column(name = "min_quality_score", precision = 8, scale = 4)
    private BigDecimal minQualityScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "quality_rules", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> qualityRules = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evaluation_metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> evaluationMetadata = new LinkedHashMap<>();

    protected LayerQualityPolicy() {
    }

    public static LayerQualityPolicy create(
            UUID pipelineLayerId,
            String policyCode,
            boolean enabled,
            int priorityOrder,
            BigDecimal minQualityScore,
            Map<String, Object> qualityRules,
            Map<String, Object> evaluationMetadata
    ) {
        LayerQualityPolicy policy = new LayerQualityPolicy();
        policy.pipelineLayerId = AiToolProvider.require(pipelineLayerId, "pipelineLayerId");
        policy.policyCode = AiToolProvider.normalizeCode(policyCode, "policyCode");
        policy.apply(enabled, priorityOrder, minQualityScore, qualityRules, evaluationMetadata);
        return policy;
    }

    public void update(
            boolean enabled,
            int priorityOrder,
            BigDecimal minQualityScore,
            Map<String, Object> qualityRules,
            Map<String, Object> evaluationMetadata
    ) {
        apply(enabled, priorityOrder, minQualityScore, qualityRules, evaluationMetadata);
    }

    public void disable() {
        this.enabled = false;
    }

    public UUID getPipelineLayerId() {
        return pipelineLayerId;
    }

    public String getPolicyCode() {
        return policyCode;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getPriorityOrder() {
        return priorityOrder;
    }

    public BigDecimal getMinQualityScore() {
        return minQualityScore;
    }

    public Map<String, Object> getQualityRules() {
        return Map.copyOf(qualityRules);
    }

    public Map<String, Object> getEvaluationMetadata() {
        return Map.copyOf(evaluationMetadata);
    }

    private void apply(
            boolean enabled,
            int priorityOrder,
            BigDecimal minQualityScore,
            Map<String, Object> qualityRules,
            Map<String, Object> evaluationMetadata
    ) {
        if (priorityOrder < 1) {
            throw new IllegalArgumentException("priorityOrder must be greater than zero");
        }
        this.enabled = enabled;
        this.priorityOrder = priorityOrder;
        this.minQualityScore = normalizeScore(minQualityScore);
        this.qualityRules = AiToolProvider.normalizeMetadata(qualityRules);
        this.evaluationMetadata = AiToolProvider.normalizeMetadata(evaluationMetadata);
    }

    private static BigDecimal normalizeScore(BigDecimal score) {
        if (score == null) {
            return null;
        }
        BigDecimal normalized = score.setScale(4, RoundingMode.HALF_UP);
        if (normalized.signum() < 0 || normalized.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("minQualityScore must be between 0 and 1");
        }
        return normalized;
    }
}
