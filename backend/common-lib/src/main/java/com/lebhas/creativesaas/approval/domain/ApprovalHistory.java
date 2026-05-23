package com.lebhas.creativesaas.approval.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "approval_history",
        schema = "platform",
        indexes = {
                @Index(name = "idx_approval_history_workflow_created_at", columnList = "approval_workflow_id,created_at"),
                @Index(name = "idx_approval_history_action_by", columnList = "action_by"),
                @Index(name = "idx_approval_history_action_type", columnList = "action_type")
        })
public class ApprovalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "approval_workflow_id", nullable = false, updatable = false)
    private UUID approvalWorkflowId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approval_workflow_id", nullable = false, insertable = false, updatable = false)
    private ApprovalWorkflow approvalWorkflow;

    @Column(name = "action_by", nullable = false, updatable = false)
    private UUID actionBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 40)
    private ApprovalAction actionType;

    @Column(name = "comments", length = 2000)
    private String comments;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ApprovalHistory() {
    }

    public static ApprovalHistory record(
            UUID approvalWorkflowId,
            UUID actionBy,
            ApprovalAction actionType,
            String comments
    ) {
        ApprovalHistory history = new ApprovalHistory();
        history.approvalWorkflowId = require(approvalWorkflowId, "approvalWorkflowId");
        history.actionBy = require(actionBy, "actionBy");
        history.actionType = require(actionType, "actionType");
        history.comments = normalizeNullable(comments);
        return history;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getApprovalWorkflowId() {
        return approvalWorkflowId;
    }

    public ApprovalWorkflow getApprovalWorkflow() {
        return approvalWorkflow;
    }

    public UUID getActionBy() {
        return actionBy;
    }

    public ApprovalAction getActionType() {
        return actionType;
    }

    public String getComments() {
        return comments;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static UUID require(UUID value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static <T> T require(T value, String field) {
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
