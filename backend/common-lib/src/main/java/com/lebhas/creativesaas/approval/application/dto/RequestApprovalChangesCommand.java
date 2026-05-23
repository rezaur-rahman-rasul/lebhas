package com.lebhas.creativesaas.approval.application.dto;

import java.util.UUID;

public record RequestApprovalChangesCommand(
        UUID workspaceId,
        UUID approvalRequestId,
        String feedback
) {
}
