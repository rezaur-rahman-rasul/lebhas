package com.lebhas.creativesaas.approval.application.dto;

import com.lebhas.creativesaas.approval.domain.CreativeApprovalPriority;
import com.lebhas.creativesaas.approval.domain.CreativeApprovalStatus;
import com.lebhas.creativesaas.profile.application.dto.SafeProfileDisplayView;

import java.time.Instant;
import java.util.UUID;

public record CreativeApprovalView(
        UUID id,
        UUID workspaceId,
        UUID creativeOutputId,
        UUID generationRequestId,
        UUID submittedBy,
        SafeProfileDisplayView submittedByDisplay,
        UUID reviewedBy,
        SafeProfileDisplayView reviewedByDisplay,
        CreativeApprovalStatus status,
        CreativeApprovalPriority priority,
        Instant submittedAt,
        Instant reviewStartedAt,
        Instant reviewedAt,
        Instant dueAt,
        String approvalNote,
        String rejectionReason,
        String regenerateInstruction,
        Instant createdAt,
        Instant updatedAt
) {
}
