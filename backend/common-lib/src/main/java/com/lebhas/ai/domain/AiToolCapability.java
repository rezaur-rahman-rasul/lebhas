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
@Table(name = "ai_tool_capabilities", schema = "platform")
public class AiToolCapability extends BaseEntity {

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "capability_code", nullable = false, length = 120)
    private String capabilityCode;

    @Column(name = "layer_code", nullable = false, length = 120)
    private String layerCode;

    @Column(name = "model_code", length = 120)
    private String modelCode;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata = new LinkedHashMap<>();

    protected AiToolCapability() {
    }

    public static AiToolCapability create(
            UUID providerId,
            String capabilityCode,
            String layerCode,
            String modelCode,
            boolean enabled,
            Map<String, Object> metadata
    ) {
        AiToolCapability capability = new AiToolCapability();
        capability.providerId = AiToolProvider.require(providerId, "providerId");
        capability.capabilityCode = AiToolProvider.normalizeCode(capabilityCode, "capabilityCode");
        capability.layerCode = AiToolProvider.normalizeCode(layerCode, "layerCode");
        capability.modelCode = modelCode == null ? null : AiToolProvider.normalizeCode(modelCode, "modelCode");
        capability.enabled = enabled;
        capability.metadata = AiToolProvider.normalizeMetadata(metadata);
        return capability;
    }

    public UUID getProviderId() {
        return providerId;
    }

    public String getCapabilityCode() {
        return capabilityCode;
    }

    public String getLayerCode() {
        return layerCode;
    }

    public String getModelCode() {
        return modelCode;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Map<String, Object> getMetadata() {
        return Map.copyOf(metadata);
    }
}
