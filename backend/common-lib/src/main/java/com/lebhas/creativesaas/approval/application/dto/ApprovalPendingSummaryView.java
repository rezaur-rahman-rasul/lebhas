package com.lebhas.creativesaas.approval.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApprovalPendingSummaryView(
        UUID workspaceId,
        int pendingCount,
        int inReviewCount,
        int changesRequestedCount,
        List<UUID> pendingApprovalRequestIds,
        Instant updatedAt
) {
}
