package com.lebhas.creativesaas.approval.application.dto;

import com.lebhas.creativesaas.approval.domain.CreativeReviewCommentType;

import java.util.UUID;

public record AddCreativeReviewCommentCommand(
        UUID workspaceId,
        UUID approvalId,
        String comment,
        CreativeReviewCommentType commentType
) {
}
