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
@Table(name = "legacy_approval_reviews", schema = "platform")
@Deprecated(forRemoval = true)
public class ApprovalReview extends TenantAwareEntity {

    @Column(name = "approval_request_id", nullable = false, updatable = false)
    private UUID approvalRequestId;

    @Column(name = "reviewer_id", nullable = false, updatable = false)
    private UUID reviewerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 40)
    private ApprovalDecision decision;

    @Column(name = "feedback", length = 2000)
    private String feedback;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_type", nullable = false, length = 40)
    private ApprovalReviewType reviewType;

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt;

    protected ApprovalReview() {
    }

    public static ApprovalReview create(
            UUID workspaceId,
            UUID approvalRequestId,
            UUID reviewerId,
            ApprovalDecision decision,
            String feedback,
            ApprovalReviewType reviewType,
            Instant reviewedAt
    ) {
        ApprovalReview review = new ApprovalReview();
        review.assignWorkspace(workspaceId);
        review.approvalRequestId = require(approvalRequestId, "approvalRequestId");
        review.reviewerId = require(reviewerId, "reviewerId");
        review.decision = decision == null ? ApprovalDecision.CHANGES_REQUESTED : decision;
        review.feedback = normalizeNullable(feedback);
        review.reviewType = reviewType == null ? ApprovalReviewType.INITIAL : reviewType;
        review.reviewedAt = reviewedAt == null ? Instant.now() : reviewedAt;
        return review;
    }

    public UUID getApprovalRequestId() {
        return approvalRequestId;
    }

    public UUID getReviewerId() {
        return reviewerId;
    }

    public ApprovalDecision getDecision() {
        return decision;
    }

    public String getFeedback() {
        return feedback;
    }

    public ApprovalReviewType getReviewType() {
        return reviewType;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
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
