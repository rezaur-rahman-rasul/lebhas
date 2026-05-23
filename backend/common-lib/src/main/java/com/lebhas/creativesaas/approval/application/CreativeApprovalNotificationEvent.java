package com.lebhas.creativesaas.approval.application;

import com.lebhas.creativesaas.approval.domain.CreativeApprovalAction;
import com.lebhas.creativesaas.approval.domain.CreativeApprovalStatus;

import java.time.Instant;
import java.util.UUID;

public record CreativeApprovalNotificationEvent(
        UUID workspaceId,
        UUID approvalId,
        UUID creativeOutputId,
        UUID generationRequestId,
        CreativeApprovalAction action,
        CreativeApprovalStatus status,
        UUID actorId,
        UUID submittedBy,
        UUID reviewedBy,
        Instant occurredAt
) {
}
