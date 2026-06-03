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
@Table(name = "creative_tool_capabilities", schema = "platform")
public class CreativeToolCapability extends BaseEntity {

    @Column(name = "tool_id", nullable = false)
    private UUID toolId;

    @Column(name = "capability_code", nullable = false, length = 120)
    private String capabilityCode;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata = new LinkedHashMap<>();

    protected CreativeToolCapability() {
    }

    public static CreativeToolCapability create(UUID toolId, String capabilityCode, boolean enabled, Map<String, Object> metadata) {
        CreativeToolCapability capability = new CreativeToolCapability();
        capability.toolId = AiToolProvider.require(toolId, "toolId");
        capability.capabilityCode = AiToolProvider.normalizeCode(capabilityCode, "capabilityCode");
        capability.enabled = enabled;
        capability.metadata = AiToolProvider.normalizeMetadata(metadata);
        return capability;
    }

    public UUID getToolId() {
        return toolId;
    }

    public String getCapabilityCode() {
        return capabilityCode;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Map<String, Object> getMetadata() {
        return Map.copyOf(metadata);
    }
}
