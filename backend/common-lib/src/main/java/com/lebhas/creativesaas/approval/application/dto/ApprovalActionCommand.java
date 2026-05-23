package com.lebhas.creativesaas.approval.application.dto;

import java.util.UUID;

public record ApprovalActionCommand(
        UUID workspaceId,
        UUID approvalWorkflowId,
        String comments
) {
}
