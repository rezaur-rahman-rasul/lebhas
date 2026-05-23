package com.lebhas.creativesaas.approval.application.dto;

import java.util.UUID;

public record AssignApprovalRequestCommand(
        UUID workspaceId,
        UUID approvalRequestId,
        UUID reviewerId
) {
}
