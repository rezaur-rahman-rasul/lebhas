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
@Table(name = "legacy_approval_requests", schema = "platform")
@Deprecated(forRemoval = true)
public class ApprovalRequest extends TenantAwareEntity {

    @Column(name = "generated_version_id", nullable = false, updatable = false)
    private UUID generatedVersionId;

    @Column(name = "project_campaign_id", nullable = false, updatable = false)
    private UUID projectCampaignId;

    @Column(name = "submitted_by", nullable = false, updatable = false)
    private UUID submittedBy;

    @Column(name = "assigned_reviewer_id")
    private UUID assignedReviewerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 40)
    private ApprovalStatus currentStatus;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "latest_comment", length = 2000)
    private String latestComment;

    @Column(name = "revision_count", nullable = false)
    private int revisionCount;

    protected ApprovalRequest() {
    }

    public static ApprovalRequest create(
            UUID workspaceId,
            UUID generatedVersionId,
            UUID projectCampaignId,
            UUID submittedBy,
            UUID assignedReviewerId,
            ApprovalStatus currentStatus,
            Instant submittedAt,
            Instant reviewedAt,
            Instant dueAt,
            String latestComment,
            int revisionCount
    ) {
        ApprovalRequest request = new ApprovalRequest();
        request.assignWorkspace(workspaceId);
        request.generatedVersionId = require(generatedVersionId, "generatedVersionId");
        request.projectCampaignId = require(projectCampaignId, "projectCampaignId");
        request.submittedBy = require(submittedBy, "submittedBy");
        request.assignedReviewerId = assignedReviewerId;
        request.currentStatus = currentStatus == null ? ApprovalStatus.NOT_SUBMITTED : currentStatus;
        request.submittedAt = submittedAt;
        request.reviewedAt = reviewedAt;
        request.dueAt = dueAt;
        request.latestComment = normalizeNullable(latestComment);
        request.revisionCount = Math.max(0, revisionCount);
        return request;
    }

    public UUID getGeneratedVersionId() {
        return generatedVersionId;
    }

    public UUID getProjectCampaignId() {
        return projectCampaignId;
    }

    public UUID getSubmittedBy() {
        return submittedBy;
    }

    public UUID getAssignedReviewerId() {
        return assignedReviewerId;
    }

    public ApprovalStatus getCurrentStatus() {
        return currentStatus;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public String getLatestComment() {
        return latestComment;
    }

    public int getRevisionCount() {
        return revisionCount;
    }

    public void assignReviewer(UUID reviewerId) {
        this.assignedReviewerId = reviewerId;
    }

    public void markSubmitted(Instant submittedAt, Instant dueAt, String latestComment) {
        this.currentStatus = ApprovalStatus.SUBMITTED;
        this.submittedAt = submittedAt == null ? Instant.now() : submittedAt;
        this.reviewedAt = null;
        this.dueAt = dueAt;
        this.latestComment = normalizeNullable(latestComment);
    }

    public void markInReview(UUID reviewerId, Instant reviewedAt, String latestComment) {
        if (reviewerId != null) {
            this.assignedReviewerId = reviewerId;
        }
        this.currentStatus = ApprovalStatus.IN_REVIEW;
        this.reviewedAt = reviewedAt == null ? Instant.now() : reviewedAt;
        this.latestComment = normalizeNullable(latestComment);
    }

    public void markReviewed(ApprovalStatus status, UUID reviewerId, Instant reviewedAt, String latestComment) {
        if (reviewerId != null) {
            this.assignedReviewerId = reviewerId;
        }
        this.currentStatus = status == null ? this.currentStatus : status;
        this.reviewedAt = reviewedAt == null ? Instant.now() : reviewedAt;
        this.latestComment = normalizeNullable(latestComment);
    }

    public void markResubmitted(Instant submittedAt, String latestComment) {
        this.currentStatus = ApprovalStatus.RESUBMITTED;
        this.submittedAt = submittedAt == null ? Instant.now() : submittedAt;
        this.reviewedAt = null;
        this.latestComment = normalizeNullable(latestComment);
        this.revisionCount = this.revisionCount + 1;
    }

    public void updateStatus(ApprovalStatus status, Instant reviewedAt) {
        this.currentStatus = status == null ? this.currentStatus : status;
        this.reviewedAt = reviewedAt;
    }

    public void updateLatestComment(String latestComment) {
        this.latestComment = normalizeNullable(latestComment);
    }

    public void incrementRevisionCount() {
        this.revisionCount = this.revisionCount + 1;
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
