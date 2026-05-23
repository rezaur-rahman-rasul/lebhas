package com.lebhas.creativesaas.approval.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "legacy_approval_assignments", schema = "platform")
@Deprecated(forRemoval = true)
public class ApprovalAssignment extends TenantAwareEntity {

    @Column(name = "approval_request_id", nullable = false, updatable = false)
    private UUID approvalRequestId;

    @Column(name = "assigned_to", nullable = false)
    private UUID assignedTo;

    @Column(name = "assigned_by", nullable = false, updatable = false)
    private UUID assignedBy;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_status", nullable = false, length = 40)
    private ApprovalAssignmentStatus assignmentStatus;

    protected ApprovalAssignment() {
    }

    public static ApprovalAssignment create(
            UUID workspaceId,
            UUID approvalRequestId,
            UUID assignedTo,
            UUID assignedBy,
            Instant assignedAt,
            ApprovalAssignmentStatus assignmentStatus
    ) {
        ApprovalAssignment assignment = new ApprovalAssignment();
        assignment.assignWorkspace(workspaceId);
        assignment.approvalRequestId = require(approvalRequestId, "approvalRequestId");
        assignment.assignedTo = require(assignedTo, "assignedTo");
        assignment.assignedBy = require(assignedBy, "assignedBy");
        assignment.assignedAt = assignedAt == null ? Instant.now() : assignedAt;
        assignment.assignmentStatus = assignmentStatus == null ? ApprovalAssignmentStatus.ACTIVE : assignmentStatus;
        return assignment;
    }

    public UUID getApprovalRequestId() {
        return approvalRequestId;
    }

    public UUID getAssignedTo() {
        return assignedTo;
    }

    public UUID getAssignedBy() {
        return assignedBy;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public ApprovalAssignmentStatus getAssignmentStatus() {
        return assignmentStatus;
    }

    public void updateAssignmentStatus(ApprovalAssignmentStatus assignmentStatus) {
        if (assignmentStatus != null) {
            this.assignmentStatus = assignmentStatus;
        }
    }

    public void reassign(UUID assignedTo, UUID assignedBy, Instant assignedAt) {
        this.assignedTo = require(assignedTo, "assignedTo");
        this.assignedBy = require(assignedBy, "assignedBy");
        this.assignedAt = assignedAt == null ? Instant.now() : assignedAt;
        this.assignmentStatus = ApprovalAssignmentStatus.REASSIGNED;
    }

    private static UUID require(UUID value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }
}
