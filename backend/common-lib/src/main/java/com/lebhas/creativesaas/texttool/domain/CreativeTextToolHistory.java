package com.lebhas.creativesaas.texttool.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
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
@Table(name = "creative_text_tool_history", schema = "platform")
public class CreativeTextToolHistory extends TenantAwareEntity {

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "text_tool_output_id")
    private UUID textToolOutputId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tool_type", nullable = false, length = 40)
    private CreativeTextToolType toolType;

    @Column(name = "tool_code", nullable = false, length = 120)
    private String toolCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CreativeTextToolStatus status;

    @Column(name = "credit_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal creditCost;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> requestPayload = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> responsePayload = new LinkedHashMap<>();

    protected CreativeTextToolHistory() {
    }

    public static CreativeTextToolHistory requested(
            UUID workspaceId,
            UUID projectId,
            CreativeTextToolType toolType,
            String toolCode,
            BigDecimal creditCost,
            Map<String, Object> requestPayload
    ) {
        CreativeTextToolHistory history = new CreativeTextToolHistory();
        history.assignWorkspace(require(workspaceId, "workspaceId"));
        history.projectId = require(projectId, "projectId");
        history.toolType = require(toolType, "toolType");
        history.toolCode = require(toolCode, "toolCode");
        history.status = CreativeTextToolStatus.REQUESTED;
        history.creditCost = require(creditCost, "creditCost");
        history.requestPayload = safeMap(requestPayload);
        history.responsePayload = new LinkedHashMap<>();
        return history;
    }

    public void complete(UUID outputId, Map<String, Object> responsePayload) {
        this.textToolOutputId = require(outputId, "outputId");
        this.status = CreativeTextToolStatus.COMPLETED;
        this.failureReason = null;
        this.responsePayload = safeMap(responsePayload);
    }

    public void fail(String failureReason) {
        this.status = CreativeTextToolStatus.FAILED;
        this.failureReason = failureReason == null ? null : failureReason.replaceAll("\\s+", " ").trim();
    }

    public UUID getProjectId() { return projectId; }
    public UUID getTextToolOutputId() { return textToolOutputId; }
    public CreativeTextToolType getToolType() { return toolType; }
    public String getToolCode() { return toolCode; }
    public CreativeTextToolStatus getStatus() { return status; }
    public BigDecimal getCreditCost() { return creditCost; }
    public String getFailureReason() { return failureReason; }
    public Map<String, Object> getRequestPayload() { return Map.copyOf(requestPayload); }
    public Map<String, Object> getResponsePayload() { return Map.copyOf(responsePayload); }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static Map<String, Object> safeMap(Map<String, Object> source) {
        return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    }
}
