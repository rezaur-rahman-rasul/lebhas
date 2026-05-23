package com.lebhas.creativesaas.approval.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ApprovalCommentView(
        UUID id,
        UUID workspaceId,
        UUID approvalRequestId,
        UUID generatedVersionId,
        UUID commentedBy,
        String commentText,
        boolean internalOnly,
        Instant createdAt,
        Instant updatedAt
) {
}
