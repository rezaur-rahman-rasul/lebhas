package com.lebhas.creativesaas.approval.application.dto;

import java.util.UUID;

public record AddApprovalCommentCommand(
        UUID workspaceId,
        UUID approvalRequestId,
        String commentText,
        boolean internalOnly
) {
}
