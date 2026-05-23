package com.lebhas.creativesaas.approval.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "legacy_creative_review_comments", schema = "platform")
@Deprecated(forRemoval = true)
public class CreativeReviewCommentEntity extends TenantAwareEntity {

    @Column(name = "approval_id", nullable = false, updatable = false)
    private UUID approvalId;

    @Column(name = "creative_output_id", nullable = false, updatable = false)
    private UUID creativeOutputId;

    @Column(name = "author_id", nullable = false, updatable = false)
    private UUID authorId;

    @Column(name = "comment", nullable = false, length = 2000)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "comment_type", nullable = false, length = 40)
    private CreativeReviewCommentType commentType;

    protected CreativeReviewCommentEntity() {
    }

    public static CreativeReviewCommentEntity create(
            UUID workspaceId,
            UUID approvalId,
            UUID creativeOutputId,
            UUID authorId,
            String comment,
            CreativeReviewCommentType commentType
    ) {
        CreativeReviewCommentEntity entity = new CreativeReviewCommentEntity();
        entity.assignWorkspace(workspaceId);
        entity.approvalId = approvalId;
        entity.creativeOutputId = creativeOutputId;
        entity.authorId = authorId;
        entity.comment = normalizeRequired(comment);
        entity.commentType = commentType == null ? CreativeReviewCommentType.GENERAL : commentType;
        return entity;
    }

    public UUID getApprovalId() {
        return approvalId;
    }

    public UUID getCreativeOutputId() {
        return creativeOutputId;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public String getComment() {
        return comment;
    }

    public CreativeReviewCommentType getCommentType() {
        return commentType;
    }

    private static String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }
}
