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
@Table(name = "layer_tool_mappings", schema = "platform")
public class LayerToolMapping extends BaseEntity {

    @Column(name = "pipeline_layer_id", nullable = false)
    private UUID pipelineLayerId;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "model_id")
    private UUID modelId;

    @Column(name = "capability_id")
    private UUID capabilityId;

    @Column(name = "mapping_code", nullable = false, length = 120)
    private String mappingCode;

    @Column(name = "priority_order", nullable = false)
    private int priorityOrder;

    @Column(name = "routing_weight", nullable = false)
    private int routingWeight;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "fallback_eligible", nullable = false)
    private boolean fallbackEligible;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "routing_metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> routingMetadata = new LinkedHashMap<>();

    protected LayerToolMapping() {
    }

    public static LayerToolMapping create(
            UUID pipelineLayerId,
            UUID providerId,
            UUID modelId,
            UUID capabilityId,
            String mappingCode,
            int priorityOrder,
            int routingWeight,
            boolean enabled,
            boolean fallbackEligible,
            Map<String, Object> routingMetadata
    ) {
        LayerToolMapping mapping = new LayerToolMapping();
        mapping.pipelineLayerId = AiToolProvider.require(pipelineLayerId, "pipelineLayerId");
        mapping.providerId = AiToolProvider.require(providerId, "providerId");
        mapping.modelId = modelId;
        mapping.capabilityId = capabilityId;
        mapping.mappingCode = AiToolProvider.normalizeCode(mappingCode, "mappingCode");
        mapping.apply(priorityOrder, routingWeight, enabled, fallbackEligible, routingMetadata);
        return mapping;
    }

    public void update(
            UUID modelId,
            UUID capabilityId,
            int priorityOrder,
            int routingWeight,
            boolean enabled,
            boolean fallbackEligible,
            Map<String, Object> routingMetadata
    ) {
        this.modelId = modelId;
        this.capabilityId = capabilityId;
        apply(priorityOrder, routingWeight, enabled, fallbackEligible, routingMetadata);
    }

    public void disable() {
        this.enabled = false;
    }

    public UUID getPipelineLayerId() {
        return pipelineLayerId;
    }

    public UUID getProviderId() {
        return providerId;
    }

    public UUID getModelId() {
        return modelId;
    }

    public UUID getCapabilityId() {
        return capabilityId;
    }

    public String getMappingCode() {
        return mappingCode;
    }

    public int getPriorityOrder() {
        return priorityOrder;
    }

    public int getRoutingWeight() {
        return routingWeight;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isFallbackEligible() {
        return fallbackEligible;
    }

    public Map<String, Object> getRoutingMetadata() {
        return Map.copyOf(routingMetadata);
    }

    private void apply(
            int priorityOrder,
            int routingWeight,
            boolean enabled,
            boolean fallbackEligible,
            Map<String, Object> routingMetadata
    ) {
        if (priorityOrder < 1) {
            throw new IllegalArgumentException("priorityOrder must be greater than zero");
        }
        if (routingWeight < 0) {
            throw new IllegalArgumentException("routingWeight must not be negative");
        }
        this.priorityOrder = priorityOrder;
        this.routingWeight = routingWeight;
        this.enabled = enabled;
        this.fallbackEligible = fallbackEligible;
        this.routingMetadata = AiToolProvider.normalizeMetadata(routingMetadata);
    }
}
