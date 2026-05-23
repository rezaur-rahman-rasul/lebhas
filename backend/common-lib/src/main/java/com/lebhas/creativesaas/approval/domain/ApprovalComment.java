package com.lebhas.creativesaas.approval.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "legacy_approval_comments", schema = "platform")
@Deprecated(forRemoval = true)
public class ApprovalComment extends TenantAwareEntity {

    @Column(name = "approval_request_id", nullable = false, updatable = false)
    private UUID approvalRequestId;

    @Column(name = "generated_version_id", nullable = false, updatable = false)
    private UUID generatedVersionId;

    @Column(name = "commented_by", nullable = false, updatable = false)
    private UUID commentedBy;

    @Column(name = "comment_text", nullable = false, length = 2000)
    private String commentText;

    @Column(name = "internal_only", nullable = false)
    private boolean internalOnly;

    protected ApprovalComment() {
    }

    public static ApprovalComment create(
            UUID workspaceId,
            UUID approvalRequestId,
            UUID generatedVersionId,
            UUID commentedBy,
            String commentText,
            boolean internalOnly
    ) {
        ApprovalComment comment = new ApprovalComment();
        comment.assignWorkspace(workspaceId);
        comment.approvalRequestId = require(approvalRequestId, "approvalRequestId");
        comment.generatedVersionId = require(generatedVersionId, "generatedVersionId");
        comment.commentedBy = require(commentedBy, "commentedBy");
        comment.commentText = normalizeRequired(commentText);
        comment.internalOnly = internalOnly;
        return comment;
    }

    public UUID getApprovalRequestId() {
        return approvalRequestId;
    }

    public UUID getGeneratedVersionId() {
        return generatedVersionId;
    }

    public UUID getCommentedBy() {
        return commentedBy;
    }

    public String getCommentText() {
        return commentText;
    }

    public boolean isInternalOnly() {
        return internalOnly;
    }

    private static UUID require(UUID value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static String normalizeRequired(String value) {
        if (value == null) {
            throw new IllegalArgumentException("commentText must not be null");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("commentText must not be blank");
        }
        return trimmed;
    }
}
