package com.lebhas.ai.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "provider_routing_policies", schema = "platform")
public class ProviderRoutingPolicy extends BaseEntity {

    @Column(name = "policy_code", nullable = false, length = 120)
    private String policyCode;

    @Column(name = "tool_id", nullable = false)
    private UUID toolId;

    @Column(name = "quality_mode", nullable = false, length = 60)
    private String qualityMode;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "model_id")
    private UUID modelId;

    @Column(name = "fallback_provider_id")
    private UUID fallbackProviderId;

    @Column(name = "fallback_model_id")
    private UUID fallbackModelId;

    @Column(name = "priority_order", nullable = false)
    private int priorityOrder;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "circuit_failure_threshold", nullable = false)
    private int circuitFailureThreshold;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata = new LinkedHashMap<>();

    protected ProviderRoutingPolicy() {
    }

    public static ProviderRoutingPolicy create(String policyCode, UUID toolId, String qualityMode, UUID providerId, UUID modelId, UUID fallbackProviderId, UUID fallbackModelId, int priorityOrder, boolean enabled, int circuitFailureThreshold, Map<String, Object> metadata) {
        ProviderRoutingPolicy policy = new ProviderRoutingPolicy();
        policy.policyCode = AiToolProvider.normalizeCode(policyCode, "policyCode");
        policy.update(toolId, qualityMode, providerId, modelId, fallbackProviderId, fallbackModelId, priorityOrder, enabled, circuitFailureThreshold, metadata);
        return policy;
    }

    public void update(UUID toolId, String qualityMode, UUID providerId, UUID modelId, UUID fallbackProviderId, UUID fallbackModelId, int priorityOrder, boolean enabled, int circuitFailureThreshold, Map<String, Object> metadata) {
        if (priorityOrder < 1 || circuitFailureThreshold < 1) {
            throw new IllegalArgumentException("priority and circuit failure threshold must be positive");
        }
        this.toolId = AiToolProvider.require(toolId, "toolId");
        this.qualityMode = AiToolProvider.normalizeCode(qualityMode, "qualityMode");
        this.providerId = AiToolProvider.require(providerId, "providerId");
        this.modelId = modelId;
        this.fallbackProviderId = fallbackProviderId;
        this.fallbackModelId = fallbackModelId;
        this.priorityOrder = priorityOrder;
        this.enabled = enabled;
        this.circuitFailureThreshold = circuitFailureThreshold;
        this.metadata = AiToolProvider.normalizeMetadata(metadata);
    }

    public String getPolicyCode() { return policyCode; }
    public UUID getToolId() { return toolId; }
    public String getQualityMode() { return qualityMode; }
    public UUID getProviderId() { return providerId; }
    public UUID getModelId() { return modelId; }
    public UUID getFallbackProviderId() { return fallbackProviderId; }
    public UUID getFallbackModelId() { return fallbackModelId; }
    public int getPriorityOrder() { return priorityOrder; }
    public boolean isEnabled() { return enabled; }
    public int getCircuitFailureThreshold() { return circuitFailureThreshold; }
    public Map<String, Object> getMetadata() { return Map.copyOf(metadata); }
}
