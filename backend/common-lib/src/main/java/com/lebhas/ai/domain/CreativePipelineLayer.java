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
@Table(name = "creative_pipeline_layers", schema = "platform")
public class CreativePipelineLayer extends BaseEntity {

    @Column(name = "pipeline_id", nullable = false)
    private UUID pipelineId;

    @Enumerated(EnumType.STRING)
    @Column(name = "layer_type", nullable = false, length = 60)
    private CreativeLayerType layerType;

    @Column(name = "layer_code", nullable = false, length = 120)
    private String layerCode;

    @Column(name = "layer_name", nullable = false, length = 180)
    private String layerName;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "required_layer", nullable = false)
    private boolean required;

    @Column(name = "retryable", nullable = false)
    private boolean retryable;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> configuration = new LinkedHashMap<>();

    protected CreativePipelineLayer() {
    }

    public static CreativePipelineLayer create(
            UUID pipelineId,
            CreativeLayerType layerType,
            String layerCode,
            String layerName,
            int sortOrder,
            boolean enabled,
            boolean required,
            boolean retryable,
            Map<String, Object> configuration
    ) {
        CreativePipelineLayer layer = new CreativePipelineLayer();
        layer.pipelineId = AiToolProvider.require(pipelineId, "pipelineId");
        layer.layerType = AiToolProvider.require(layerType, "layerType");
        layer.layerCode = AiToolProvider.normalizeCode(layerCode, "layerCode");
        layer.apply(layerName, sortOrder, enabled, required, retryable, configuration);
        return layer;
    }

    public void update(
            String layerName,
            int sortOrder,
            boolean enabled,
            boolean required,
            boolean retryable,
            Map<String, Object> configuration
    ) {
        apply(layerName, sortOrder, enabled, required, retryable, configuration);
    }

    public UUID getPipelineId() {
        return pipelineId;
    }

    public CreativeLayerType getLayerType() {
        return layerType;
    }

    public String getLayerCode() {
        return layerCode;
    }

    public String getLayerName() {
        return layerName;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public Map<String, Object> getConfiguration() {
        return Map.copyOf(configuration);
    }

    private void apply(
            String layerName,
            int sortOrder,
            boolean enabled,
            boolean required,
            boolean retryable,
            Map<String, Object> configuration
    ) {
        if (sortOrder < 1) {
            throw new IllegalArgumentException("sortOrder must be greater than zero");
        }
        this.layerName = AiToolProvider.normalizeRequired(layerName, "layerName");
        this.sortOrder = sortOrder;
        this.enabled = enabled;
        this.required = required;
        this.retryable = retryable;
        this.configuration = AiToolProvider.normalizeMetadata(configuration);
    }
}
