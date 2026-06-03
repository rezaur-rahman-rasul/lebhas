package com.lebhas.creativesaas.campaignpackage.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "bulk_generation_jobs", schema = "platform")
public class BulkGenerationJob extends TenantAwareEntity {

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_type", nullable = false, length = 40)
    private BulkGenerationType generationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", length = 40)
    private PromptPlatform platform;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", length = 40)
    private PromptLanguage language;

    @Column(name = "item_count", nullable = false)
    private int itemCount;

    @Column(name = "estimated_credits", precision = 19, scale = 4, nullable = false)
    private BigDecimal estimatedCredits;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private BulkGenerationJobStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_payload", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> requestPayload = new LinkedHashMap<>();

    protected BulkGenerationJob() {
    }

    public static BulkGenerationJob queued(UUID workspaceId, UUID projectId, BulkGenerationType type, PromptPlatform platform,
                                           PromptLanguage language, int itemCount, BigDecimal estimatedCredits, Map<String, Object> payload) {
        BulkGenerationJob job = new BulkGenerationJob();
        job.assignWorkspace(workspaceId);
        job.projectId = projectId;
        job.generationType = type;
        job.platform = platform;
        job.language = language;
        job.itemCount = itemCount;
        job.estimatedCredits = estimatedCredits == null ? BigDecimal.ZERO : estimatedCredits;
        job.status = BulkGenerationJobStatus.QUEUED;
        job.requestPayload = new LinkedHashMap<>(payload == null ? Map.of() : payload);
        return job;
    }

    public UUID getProjectId() { return projectId; }
    public BulkGenerationType getGenerationType() { return generationType; }
    public PromptPlatform getPlatform() { return platform; }
    public PromptLanguage getLanguage() { return language; }
    public int getItemCount() { return itemCount; }
    public BigDecimal getEstimatedCredits() { return estimatedCredits; }
    public BulkGenerationJobStatus getStatus() { return status; }
    public Map<String, Object> getRequestPayload() { return Map.copyOf(requestPayload); }
}
