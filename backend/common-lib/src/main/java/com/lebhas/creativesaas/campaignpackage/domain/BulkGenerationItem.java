package com.lebhas.creativesaas.campaignpackage.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
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
@Table(name = "bulk_generation_items", schema = "platform")
public class BulkGenerationItem extends TenantAwareEntity {

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "source_id")
    private UUID sourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private BulkGenerationItemStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "item_payload", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> itemPayload = new LinkedHashMap<>();

    protected BulkGenerationItem() {
    }

    public static BulkGenerationItem queued(UUID workspaceId, UUID jobId, UUID projectId, UUID sourceId, Map<String, Object> payload) {
        BulkGenerationItem item = new BulkGenerationItem();
        item.assignWorkspace(workspaceId);
        item.jobId = jobId;
        item.projectId = projectId;
        item.sourceId = sourceId;
        item.status = BulkGenerationItemStatus.QUEUED;
        item.itemPayload = new LinkedHashMap<>(payload == null ? Map.of() : payload);
        return item;
    }

    public UUID getJobId() { return jobId; }
    public UUID getProjectId() { return projectId; }
    public UUID getSourceId() { return sourceId; }
    public BulkGenerationItemStatus getStatus() { return status; }
    public Map<String, Object> getItemPayload() { return Map.copyOf(itemPayload); }
}
