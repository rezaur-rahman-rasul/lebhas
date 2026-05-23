package com.lebhas.creativesaas.approval.application.dto;

import java.time.Instant;
import java.util.UUID;

public record SubmitApprovalRequestCommand(
        UUID workspaceId,
        UUID generatedVersionId,
        Instant dueAt,
        String submissionComment
) {
}
