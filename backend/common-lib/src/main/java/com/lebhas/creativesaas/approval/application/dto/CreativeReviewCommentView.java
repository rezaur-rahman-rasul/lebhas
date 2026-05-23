package com.lebhas.creativesaas.approval.application.dto;

import com.lebhas.creativesaas.approval.domain.CreativeReviewCommentType;

import java.time.Instant;
import java.util.UUID;

public record CreativeReviewCommentView(
        UUID id,
        UUID workspaceId,
        UUID approvalId,
        UUID creativeOutputId,
        UUID authorId,
        String comment,
        CreativeReviewCommentType commentType,
        Instant createdAt,
        Instant updatedAt
) {
}
