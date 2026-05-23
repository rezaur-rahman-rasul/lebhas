package com.lebhas.creativesaas.approval.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "legacy_approval_audit_logs", schema = "platform")
@Deprecated(forRemoval = true)
public class ApprovalAuditLog extends TenantAwareEntity {

    @Column(name = "event_id", length = 120)
    private String eventId;

    @Column(name = "approval_request_id", nullable = false, updatable = false)
    private UUID approvalRequestId;

    @Column(name = "generated_version_id", nullable = false, updatable = false)
    private UUID generatedVersionId;

    @Column(name = "actor_id", nullable = false, updatable = false)
    private UUID actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, updatable = false, length = 40)
    private ApprovalAuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 40)
    private ApprovalStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 40)
    private ApprovalStatus newStatus;

    @Column(name = "details", length = 2000)
    private String details;

    protected ApprovalAuditLog() {
    }

    public static ApprovalAuditLog create(
            UUID workspaceId,
            UUID approvalRequestId,
            UUID generatedVersionId,
            UUID actorId,
            ApprovalAuditAction action,
            ApprovalStatus previousStatus,
            ApprovalStatus newStatus,
            String details
    ) {
        return create(
                null,
                workspaceId,
                approvalRequestId,
                generatedVersionId,
                actorId,
                action,
                previousStatus,
                newStatus,
                details);
    }

    public static ApprovalAuditLog create(
            String eventId,
            UUID workspaceId,
            UUID approvalRequestId,
            UUID generatedVersionId,
            UUID actorId,
            ApprovalAuditAction action,
            ApprovalStatus previousStatus,
            ApprovalStatus newStatus,
            String details
    ) {
        ApprovalAuditLog auditLog = new ApprovalAuditLog();
        auditLog.assignWorkspace(workspaceId);
        auditLog.eventId = normalizeNullable(eventId);
        auditLog.approvalRequestId = require(approvalRequestId, "approvalRequestId");
        auditLog.generatedVersionId = require(generatedVersionId, "generatedVersionId");
        auditLog.actorId = require(actorId, "actorId");
        auditLog.action = action == null ? ApprovalAuditAction.SUBMITTED : action;
        auditLog.previousStatus = previousStatus;
        auditLog.newStatus = newStatus;
        auditLog.details = normalizeNullable(details);
        return auditLog;
    }

    public String getEventId() {
        return eventId;
    }

    public UUID getApprovalRequestId() {
        return approvalRequestId;
    }

    public UUID getGeneratedVersionId() {
        return generatedVersionId;
    }

    public UUID getActorId() {
        return actorId;
    }

    public ApprovalAuditAction getAction() {
        return action;
    }

    public ApprovalStatus getPreviousStatus() {
        return previousStatus;
    }

    public ApprovalStatus getNewStatus() {
        return newStatus;
    }

    public String getDetails() {
        return details;
    }

    private static UUID require(UUID value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
