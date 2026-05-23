package com.lebhas.creativesaas.approval.application.dto;

import com.lebhas.creativesaas.approval.domain.ApprovalStatus;

import java.time.Instant;
import java.util.UUID;

public record ApprovalRequestView(
        UUID id,
        UUID workspaceId,
        UUID generatedVersionId,
        UUID projectCampaignId,
        UUID submittedBy,
        UUID assignedReviewerId,
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
