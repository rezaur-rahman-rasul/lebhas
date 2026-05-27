package com.lebhas.creativesaas.approval.application.dto;

import com.lebhas.creativesaas.approval.domain.ApprovalStatus;
import com.lebhas.creativesaas.profile.application.dto.SafeProfileDisplayView;

import java.time.Instant;
import java.util.UUID;

public record ApprovalRequestView(
        UUID id,
        UUID workspaceId,
        UUID generatedVersionId,
        UUID projectCampaignId,
        UUID submittedBy,
        SafeProfileDisplayView submittedByDisplay,
        UUID assignedReviewerId,
        SafeProfileDisplayView assignedReviewerDisplay,
        ApprovalStatus currentStatus,
        Instant submittedAt,
        Instant reviewedAt,
        Instant dueAt,
        String latestComment,
        int revisionCount,
        Instant createdAt,
        Instant updatedAt
) {
}
