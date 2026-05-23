package com.lebhas.creativesaas.approval.cache;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApprovalPendingCacheEntry(
        UUID workspaceId,
        int pendingCount,
        int inReviewCount,
        int changesRequestedCount,
        List<UUID> pendingApprovalRequestIds,
        Instant updatedAt
) {

    public ApprovalPendingCacheEntry {
        pendingApprovalRequestIds = pendingApprovalRequestIds == null ? List.of() : List.copyOf(pendingApprovalRequestIds);
    }
}
