package com.lebhas.ai.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "layer_routing_policies", schema = "platform")
public class LayerRoutingPolicy extends BaseEntity {

    @Column(name = "pipeline_layer_id", nullable = false)
    private UUID pipelineLayerId;

    @Column(name = "policy_code", nullable = false, length = 120)
    private String policyCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "routing_strategy", nullable = false, length = 40)
    private LayerRoutingStrategy routingStrategy;

    @Column(name = "priority_order", nullable = false)
    private int priorityOrder;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conditions", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> conditions = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rules", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> rules = new LinkedHashMap<>();

    protected LayerRoutingPolicy() {
    }

    public static LayerRoutingPolicy create(
            UUID pipelineLayerId,
            String policyCode,
            LayerRoutingStrategy routingStrategy,
            int priorityOrder,
            boolean enabled,
            Map<String, Object> conditions,
            Map<String, Object> rules
    ) {
        LayerRoutingPolicy policy = new LayerRoutingPolicy();
        policy.pipelineLayerId = AiToolProvider.require(pipelineLayerId, "pipelineLayerId");
        policy.policyCode = AiToolProvider.normalizeCode(policyCode, "policyCode");
        policy.routingStrategy = routingStrategy == null ? LayerRoutingStrategy.PRIORITY : routingStrategy;
        policy.apply(priorityOrder, enabled, conditions, rules);
        return policy;
    }

    public void update(
            LayerRoutingStrategy routingStrategy,
            int priorityOrder,
            boolean enabled,
            Map<String, Object> conditions,
            Map<String, Object> rules
    ) {
        this.routingStrategy = routingStrategy == null ? LayerRoutingStrategy.PRIORITY : routingStrategy;
        apply(priorityOrder, enabled, conditions, rules);
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

    public LayerRoutingStrategy getRoutingStrategy() {
        return routingStrategy;
    }

    public int getPriorityOrder() {
        return priorityOrder;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Map<String, Object> getConditions() {
        return Map.copyOf(conditions);
    }

    public Map<String, Object> getRules() {
        return Map.copyOf(rules);
    }

    private void apply(int priorityOrder, boolean enabled, Map<String, Object> conditions, Map<String, Object> rules) {
        if (priorityOrder < 1) {
            throw new IllegalArgumentException("priorityOrder must be greater than zero");
        }
        this.priorityOrder = priorityOrder;
        this.enabled = enabled;
        this.conditions = AiToolProvider.normalizeMetadata(conditions);
        this.rules = AiToolProvider.normalizeMetadata(rules);
    }
}
