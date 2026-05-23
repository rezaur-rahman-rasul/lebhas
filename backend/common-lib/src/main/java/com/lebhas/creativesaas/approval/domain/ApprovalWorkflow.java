package com.lebhas.creativesaas.approval.domain;

import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "approval_workflows",
        schema = "platform",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_approval_workflows_workspace_generated_version",
                        columnNames = {"workspace_id", "generated_version_id"})
        },
        indexes = {
                @Index(name = "idx_approval_workflows_workspace_id", columnList = "workspace_id"),
                @Index(name = "idx_approval_workflows_creative_request_id", columnList = "creative_request_id"),
                @Index(name = "idx_approval_workflows_generated_version_id", columnList = "generated_version_id"),
                @Index(name = "idx_approval_workflows_workspace_status_created_at", columnList = "workspace_id,current_status,created_at"),
                @Index(name = "idx_approval_workflows_workspace_reviewer_created_at", columnList = "workspace_id,current_reviewer_id,created_at")
        })
public class ApprovalWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    @Column(name = "creative_request_id", nullable = false, updatable = false)
    private UUID creativeRequestId;

    @Column(name = "generated_version_id", nullable = false, updatable = false)
    private UUID generatedVersionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generated_version_id", nullable = false, insertable = false, updatable = false)
    private GeneratedVersionEntity generatedVersion;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 40)
    private ApprovalStatus currentStatus;

    @Column(name = "current_reviewer_id")
    private UUID currentReviewerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ApprovalWorkflow() {
    }

    public static ApprovalWorkflow create(
            UUID workspaceId,
            UUID creativeRequestId,
            UUID generatedVersionId,
            UUID createdBy,
            UUID currentReviewerId
    ) {
        ApprovalWorkflow workflow = new ApprovalWorkflow();
        workflow.workspaceId = require(workspaceId, "workspaceId");
        workflow.creativeRequestId = require(creativeRequestId, "creativeRequestId");
        workflow.generatedVersionId = require(generatedVersionId, "generatedVersionId");
        workflow.createdBy = require(createdBy, "createdBy");
        workflow.currentStatus = ApprovalStatus.PENDING;
        workflow.currentReviewerId = currentReviewerId;
        return workflow;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public UUID getCreativeRequestId() {
        return creativeRequestId;
    }

    public UUID getGeneratedVersionId() {
        return generatedVersionId;
    }

    public GeneratedVersionEntity getGeneratedVersion() {
        return generatedVersion;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public ApprovalStatus getCurrentStatus() {
        return currentStatus;
    }

    public UUID getCurrentReviewerId() {
        return currentReviewerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void assignReviewer(UUID currentReviewerId) {
        this.currentReviewerId = currentReviewerId;
        this.currentStatus = ApprovalStatus.IN_REVIEW;
    }

    public void markApproved() {
        this.currentStatus = ApprovalStatus.APPROVED;
    }

    public void markRejected() {
        this.currentStatus = ApprovalStatus.REJECTED;
    }

    public void requestRevision() {
        this.currentStatus = ApprovalStatus.REVISION_REQUESTED;
    }

    private static UUID require(UUID value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }
}
