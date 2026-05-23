package com.lebhas.creativesaas.approval.application.dto;

import com.lebhas.creativesaas.approval.domain.ApprovalStatus;

import java.time.Instant;
import java.util.UUID;

public record ApprovalWorkflowView(
        UUID id,
        UUID workspaceId,
        UUID creativeRequestId,
        UUID generatedVersionId,
        UUID createdBy,
        ApprovalStatus currentStatus,
        UUID currentReviewerId,
        Instant createdAt,
        Instant updatedAt
) {
}
