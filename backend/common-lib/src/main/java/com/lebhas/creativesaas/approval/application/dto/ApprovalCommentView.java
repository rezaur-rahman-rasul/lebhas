package com.lebhas.creativesaas.approval.application.dto;

import com.lebhas.creativesaas.profile.application.dto.SafeProfileDisplayView;

import java.time.Instant;
import java.util.UUID;

public record ApprovalCommentView(
        UUID id,
        UUID workspaceId,
        UUID approvalRequestId,
        UUID generatedVersionId,
        UUID commentedBy,
        SafeProfileDisplayView commentedByDisplay,
        String commentText,
        boolean internalOnly,
        Instant createdAt,
        Instant updatedAt
) {
}
