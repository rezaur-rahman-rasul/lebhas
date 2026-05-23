package com.lebhas.creativesaas.approval.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApprovalReviewerQueueView(
        UUID reviewerId,
        UUID workspaceId,
        int pendingCount,
        int inReviewCount,
        List<UUID> approvalRequestIds,
        Instant updatedAt
) {
}
