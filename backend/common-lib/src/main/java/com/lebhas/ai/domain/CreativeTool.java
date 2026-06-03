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

@Entity
@Table(name = "creative_tools", schema = "platform")
public class CreativeTool extends BaseEntity {

    @Column(name = "tool_code", nullable = false, length = 120)
    private String toolCode;

    @Column(name = "tool_name", nullable = false, length = 180)
    private String toolName;

    @Enumerated(EnumType.STRING)
    @Column(name = "tool_category", nullable = false, length = 60)
    private CreativeToolCategory toolCategory;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "description")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata = new LinkedHashMap<>();

    protected CreativeTool() {
    }

    public static CreativeTool create(String toolCode, String toolName, CreativeToolCategory toolCategory, boolean enabled, String description, Map<String, Object> metadata) {
        CreativeTool tool = new CreativeTool();
        tool.toolCode = AiToolProvider.normalizeCode(toolCode, "toolCode");
        tool.update(toolName, toolCategory, enabled, description, metadata);
        return tool;
    }

    public void update(String toolName, CreativeToolCategory toolCategory, boolean enabled, String description, Map<String, Object> metadata) {
        this.toolName = AiToolProvider.normalizeRequired(toolName, "toolName");
        this.toolCategory = AiToolProvider.require(toolCategory, "toolCategory");
        this.enabled = enabled;
        this.description = AiToolProvider.normalizeNullable(description);
        this.metadata = AiToolProvider.normalizeMetadata(metadata);
    }

    public String getToolCode() {
        return toolCode;
    }

    public String getToolName() {
        return toolName;
    }

    public CreativeToolCategory getToolCategory() {
        return toolCategory;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Object> getMetadata() {
        return Map.copyOf(metadata);
    }
}
