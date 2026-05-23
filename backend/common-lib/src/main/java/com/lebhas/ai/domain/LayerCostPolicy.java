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
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "layer_cost_policies", schema = "platform")
public class LayerCostPolicy extends BaseEntity {

    @Column(name = "pipeline_layer_id", nullable = false)
    private UUID pipelineLayerId;

    @Column(name = "policy_code", nullable = false, length = 120)
    private String policyCode;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "priority_order", nullable = false)
    private int priorityOrder;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "max_cost_per_run", precision = 19, scale = 6)
    private BigDecimal maxCostPerRun;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cost_rules", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> costRules = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "budget_metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> budgetMetadata = new LinkedHashMap<>();

    protected LayerCostPolicy() {
    }

    public static LayerCostPolicy create(
            UUID pipelineLayerId,
            String policyCode,
            boolean enabled,
            int priorityOrder,
            String currency,
            BigDecimal maxCostPerRun,
            Map<String, Object> costRules,
            Map<String, Object> budgetMetadata
    ) {
        LayerCostPolicy policy = new LayerCostPolicy();
        policy.pipelineLayerId = AiToolProvider.require(pipelineLayerId, "pipelineLayerId");
        policy.policyCode = AiToolProvider.normalizeCode(policyCode, "policyCode");
        policy.apply(enabled, priorityOrder, currency, maxCostPerRun, costRules, budgetMetadata);
        return policy;
    }

    public void update(
            boolean enabled,
            int priorityOrder,
            String currency,
            BigDecimal maxCostPerRun,
            Map<String, Object> costRules,
            Map<String, Object> budgetMetadata
    ) {
        apply(enabled, priorityOrder, currency, maxCostPerRun, costRules, budgetMetadata);
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

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getMaxCostPerRun() {
        return maxCostPerRun;
    }

    public Map<String, Object> getCostRules() {
        return Map.copyOf(costRules);
    }

    public Map<String, Object> getBudgetMetadata() {
        return Map.copyOf(budgetMetadata);
    }

    private void apply(
            boolean enabled,
            int priorityOrder,
            String currency,
            BigDecimal maxCostPerRun,
            Map<String, Object> costRules,
            Map<String, Object> budgetMetadata
    ) {
        if (priorityOrder < 1) {
            throw new IllegalArgumentException("priorityOrder must be greater than zero");
        }
        this.enabled = enabled;
        this.priorityOrder = priorityOrder;
        this.currency = normalizeCurrency(currency);
        this.maxCostPerRun = normalizeMoney(maxCostPerRun);
        this.costRules = AiToolProvider.normalizeMetadata(costRules);
        this.budgetMetadata = AiToolProvider.normalizeMetadata(budgetMetadata);
    }

    private static String normalizeCurrency(String value) {
        String normalized = AiToolProvider.normalizeRequired(value, "currency").toUpperCase(Locale.ROOT);
        if (normalized.length() != 3) {
            throw new IllegalArgumentException("currency must be a 3-letter code");
        }
        return normalized;
    }

    private static BigDecimal normalizeMoney(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        BigDecimal normalized = amount.setScale(6, RoundingMode.HALF_UP);
        if (normalized.signum() < 0) {
            throw new IllegalArgumentException("maxCostPerRun must not be negative");
        }
        return normalized;
    }
}
