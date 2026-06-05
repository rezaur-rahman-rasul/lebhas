package com.lebhas.creativesaas.generation.domain;

import com.lebhas.ai.domain.CreativeLayerType;
import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "creative_pipeline_layer_runs", schema = "platform")
public class CreativePipelineLayerRunEntity extends BaseEntity {

    @Column(name = "pipeline_run_id", nullable = false)
    private UUID pipelineRunId;

    @Column(name = "creative_request_id", nullable = false)
    private UUID creativeRequestId;

    @Column(name = "sequence_number", nullable = false)
    private int sequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "layer_type", nullable = false, length = 60)
    private CreativeLayerType layerType;

    @Column(name = "provider_code", nullable = false, length = 80)
    private String providerCode;

    @Column(name = "model_code", length = 120)
    private String modelCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CreativePipelineRunStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> inputJson = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> outputJson = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_asset_ids", nullable = false, columnDefinition = "jsonb")
    private List<UUID> inputAssetIds = List.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output_asset_ids", nullable = false, columnDefinition = "jsonb")
    private List<UUID> outputAssetIds = List.of();

    @Column(name = "estimated_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal estimatedCost = BigDecimal.ZERO;

    @Column(name = "actual_cost", precision = 19, scale = 4)
    private BigDecimal actualCost;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    protected CreativePipelineLayerRunEntity() {
    }

    public static CreativePipelineLayerRunEntity planned(
            UUID pipelineRunId,
            UUID creativeRequestId,
            int sequence,
            CreativeLayerType layerType,
            String providerCode,
            String modelCode,
            List<UUID> inputAssetIds,
            BigDecimal estimatedCost
    ) {
        CreativePipelineLayerRunEntity run = new CreativePipelineLayerRunEntity();
        run.pipelineRunId = pipelineRunId;
        run.creativeRequestId = creativeRequestId;
        run.sequence = sequence;
        run.layerType = layerType;
        run.providerCode = required(providerCode, "providerCode");
        run.modelCode = modelCode == null || modelCode.isBlank() ? null : modelCode.trim();
        run.status = CreativePipelineRunStatus.PLANNED;
        run.inputAssetIds = inputAssetIds == null ? List.of() : List.copyOf(inputAssetIds);
        run.estimatedCost = estimatedCost == null ? BigDecimal.ZERO : estimatedCost;
        return run;
    }

    public void markStarted(Map<String, Object> inputJson) {
        this.status = CreativePipelineRunStatus.PROCESSING;
        this.startedAt = Instant.now();
        this.inputJson = inputJson == null ? new LinkedHashMap<>() : new LinkedHashMap<>(inputJson);
    }

    public void markCompleted(Map<String, Object> outputJson, List<UUID> outputAssetIds, BigDecimal actualCost) {
        this.status = CreativePipelineRunStatus.COMPLETED;
        this.outputJson = outputJson == null ? new LinkedHashMap<>() : new LinkedHashMap<>(outputJson);
        this.outputAssetIds = outputAssetIds == null ? List.of() : List.copyOf(outputAssetIds);
        this.actualCost = actualCost;
        this.failureReason = null;
        this.completedAt = Instant.now();
    }

    public void markSkipped(String reason) {
        this.status = CreativePipelineRunStatus.SKIPPED;
        this.failureReason = normalize(reason);
        this.completedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = CreativePipelineRunStatus.FAILED;
        this.failureReason = normalize(reason);
        this.completedAt = Instant.now();
    }

    public UUID getPipelineRunId() { return pipelineRunId; }
    public UUID getCreativeRequestId() { return creativeRequestId; }
    public int getSequence() { return sequence; }
    public CreativeLayerType getLayerType() { return layerType; }
    public String getProviderCode() { return providerCode; }
    public String getModelCode() { return modelCode; }
    public CreativePipelineRunStatus getStatus() { return status; }
    public Map<String, Object> getInputJson() { return Map.copyOf(inputJson); }
    public Map<String, Object> getOutputJson() { return Map.copyOf(outputJson); }
    public List<UUID> getInputAssetIds() { return List.copyOf(inputAssetIds); }
    public List<UUID> getOutputAssetIds() { return List.copyOf(outputAssetIds); }
    public BigDecimal getEstimatedCost() { return estimatedCost; }
    public BigDecimal getActualCost() { return actualCost; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public String getFailureReason() { return failureReason; }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 1000 ? normalized : normalized.substring(0, 1000);
    }
}
