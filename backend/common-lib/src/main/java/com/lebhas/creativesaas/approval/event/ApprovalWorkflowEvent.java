package com.lebhas.creativesaas.approval.event;

import com.lebhas.creativesaas.approval.domain.ApprovalStatus;

import java.time.Instant;
import java.util.UUID;

public record ApprovalWorkflowEvent(
        UUID workflowId,
        UUID workspaceId,
        UUID creativeRequestId,
        UUID generatedVersionId,
        UUID actorId,
        UUID reviewerId,
        ApprovalStatus status,
        String comments,
        Instant occurredAt
) {
}
