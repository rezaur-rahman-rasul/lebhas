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
@Table(name = "creative_pipelines", schema = "platform")
public class CreativePipeline extends BaseEntity {

    @Column(name = "pipeline_code", nullable = false, length = 120)
    private String pipelineCode;

    @Column(name = "pipeline_name", nullable = false, length = 180)
    private String pipelineName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CreativePipelineStatus status;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "pipeline_version", nullable = false)
    private int version;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata = new LinkedHashMap<>();

    protected CreativePipeline() {
    }

    public static CreativePipeline create(
            String pipelineCode,
            String pipelineName,
            String description,
            CreativePipelineStatus status,
            boolean active,
            int version,
            Map<String, Object> metadata
    ) {
        CreativePipeline pipeline = new CreativePipeline();
        pipeline.pipelineCode = AiToolProvider.normalizeCode(pipelineCode, "pipelineCode");
        pipeline.apply(pipelineName, description, status, active, version, metadata);
        return pipeline;
    }

    public void update(
            String pipelineName,
            String description,
            CreativePipelineStatus status,
            boolean active,
            int version,
            Map<String, Object> metadata
    ) {
        apply(pipelineName, description, status, active, version, metadata);
    }

    public void activate() {
        this.active = true;
        this.status = CreativePipelineStatus.ACTIVE;
    }

    public void deactivate() {
        this.active = false;
        if (this.status == CreativePipelineStatus.ACTIVE) {
            this.status = CreativePipelineStatus.DISABLED;
        }
    }

    public String getPipelineCode() {
        return pipelineCode;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public String getDescription() {
        return description;
    }

    public CreativePipelineStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return active;
    }

    public int getVersion() {
        return version;
    }

    public Map<String, Object> getMetadata() {
        return Map.copyOf(metadata);
    }

    private void apply(
            String pipelineName,
            String description,
            CreativePipelineStatus status,
            boolean active,
            int version,
            Map<String, Object> metadata
    ) {
        if (version < 1) {
            throw new IllegalArgumentException("version must be greater than zero");
        }
        this.pipelineName = AiToolProvider.normalizeRequired(pipelineName, "pipelineName");
        this.description = AiToolProvider.normalizeNullable(description);
        this.status = status == null ? (active ? CreativePipelineStatus.ACTIVE : CreativePipelineStatus.DRAFT) : status;
        this.active = active;
        this.version = version;
        this.metadata = AiToolProvider.normalizeMetadata(metadata);
    }
}
