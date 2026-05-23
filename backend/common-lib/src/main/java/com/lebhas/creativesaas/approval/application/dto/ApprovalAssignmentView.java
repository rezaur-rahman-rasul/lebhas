package com.lebhas.creativesaas.approval.application.dto;

import com.lebhas.creativesaas.approval.domain.ApprovalAssignmentStatus;

import java.time.Instant;
import java.util.UUID;

public record ApprovalAssignmentView(
        UUID id,
        UUID workspaceId,
        UUID approvalRequestId,
        UUID assignedTo,
        UUID assignedBy,
        Instant assignedAt,
        ApprovalAssignmentStatus assignmentStatus,
        Instant createdAt,
        Instant updatedAt
) {
}
