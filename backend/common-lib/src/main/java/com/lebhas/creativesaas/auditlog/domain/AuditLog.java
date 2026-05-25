package com.lebhas.creativesaas.auditlog.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "audit_logs",
        schema = "platform",
        uniqueConstraints = @UniqueConstraint(name = "uk_audit_logs_source_event_id", columnNames = "source_event_id"),
        indexes = {
                @Index(name = "idx_audit_logs_workspace_created_at", columnList = "workspace_id,audit_at"),
                @Index(name = "idx_audit_logs_actor_created_at", columnList = "actor_user_id,audit_at"),
                @Index(name = "idx_audit_logs_reference", columnList = "entity_type,entity_id"),
                @Index(name = "idx_audit_logs_action_outcome", columnList = "action_type,outcome")
        })
public class AuditLog extends TenantAwareEntity {

    @Column(name = "source_event_id", nullable = false, updatable = false, length = 120)
    private String sourceEventId;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 40)
    private AuditActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 30)
    private AuditOutcome outcome;

    @Column(name = "entity_type", nullable = false, length = 80)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "summary", nullable = false, length = 500)
    private String summary;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "ip_address", length = 80)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "audit_at", nullable = false)
    private Instant auditAt;

    protected AuditLog() {
    }

    public static AuditLog create(
            UUID workspaceId,
            String sourceEventId,
            UUID actorUserId,
            AuditActionType actionType,
            AuditOutcome outcome,
            String entityType,
            UUID entityId,
            String summary,
            String metadataJson,
            String ipAddress,
            String userAgent,
            Instant auditAt
    ) {
        AuditLog auditLog = new AuditLog();
        auditLog.assignWorkspace(workspaceId);
        auditLog.sourceEventId = normalizeRequired(sourceEventId, "sourceEventId");
        auditLog.actorUserId = actorUserId;
        auditLog.actionType = require(actionType, "actionType");
        auditLog.outcome = require(outcome, "outcome");
        auditLog.entityType = normalizeRequired(entityType, "entityType");
        auditLog.entityId = entityId;
        auditLog.summary = normalizeRequired(summary, "summary");
        auditLog.metadataJson = normalizeNullable(metadataJson);
        auditLog.ipAddress = normalizeNullable(ipAddress);
        auditLog.userAgent = normalizeNullable(userAgent);
        auditLog.auditAt = auditAt == null ? Instant.now() : auditAt;
        return auditLog;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public AuditActionType getActionType() {
        return actionType;
    }

    public AuditOutcome getOutcome() {
        return outcome;
    }

    public String getEntityType() {
        return entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public String getSummary() {
        return summary;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Instant getAuditAt() {
        return auditAt;
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
