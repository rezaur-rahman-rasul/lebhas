package com.lebhas.creativesaas.approval.cache;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApprovalReviewerCacheEntry(
        UUID reviewerId,
        List<WorkspaceQueueState> workspaceQueues,
        Instant updatedAt
) {

    public ApprovalReviewerCacheEntry {
        workspaceQueues = workspaceQueues == null ? List.of() : List.copyOf(workspaceQueues);
    }

    public record WorkspaceQueueState(
            UUID workspaceId,
            int pendingCount,
            int inReviewCount,
            List<UUID> approvalRequestIds
    ) {
        public WorkspaceQueueState {
            approvalRequestIds = approvalRequestIds == null ? List.of() : List.copyOf(approvalRequestIds);
        }
    }
}
