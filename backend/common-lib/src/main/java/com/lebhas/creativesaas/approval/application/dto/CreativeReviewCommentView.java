package com.lebhas.creativesaas.approval.application.dto;

import com.lebhas.creativesaas.approval.domain.CreativeReviewCommentType;
import com.lebhas.creativesaas.profile.application.dto.SafeProfileDisplayView;

import java.time.Instant;
import java.util.UUID;

public record CreativeReviewCommentView(
        UUID id,
        UUID workspaceId,
        UUID approvalId,
        UUID creativeOutputId,
        UUID authorId,
        SafeProfileDisplayView authorDisplay,
        String comment,
        CreativeReviewCommentType commentType,
        Instant createdAt,
        Instant updatedAt
) {
}
