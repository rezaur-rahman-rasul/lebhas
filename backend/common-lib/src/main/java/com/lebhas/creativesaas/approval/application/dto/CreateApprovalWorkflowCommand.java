package com.lebhas.creativesaas.approval.application.dto;

import java.util.UUID;

public record CreateApprovalWorkflowCommand(
        UUID workspaceId,
        UUID creativeRequestId,
        UUID generatedVersionId,
        UUID currentReviewerId
) {
}
