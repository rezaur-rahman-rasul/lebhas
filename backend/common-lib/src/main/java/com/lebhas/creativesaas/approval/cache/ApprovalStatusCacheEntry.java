package com.lebhas.creativesaas.approval.cache;

import com.lebhas.creativesaas.approval.domain.ApprovalStatus;

import java.time.Instant;
import java.util.UUID;

public record ApprovalStatusCacheEntry(
        UUID generatedVersionId,
        UUID approvalRequestId,
        UUID workspaceId,
        ApprovalStatus currentStatus,
        UUID latestReviewerId,
        Instant submittedForApprovalAt,
        Instant approvalCompletedAt,
        int revisionNumber,
        Instant updatedAt
) {
}
