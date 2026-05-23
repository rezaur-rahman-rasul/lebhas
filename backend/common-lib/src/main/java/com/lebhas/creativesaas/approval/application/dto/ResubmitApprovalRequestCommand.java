package com.lebhas.creativesaas.approval.application.dto;

import java.util.UUID;

public record ResubmitApprovalRequestCommand(
        UUID workspaceId,
        UUID approvalRequestId,
        String resubmissionComment
) {
}
