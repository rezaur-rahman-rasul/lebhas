package com.lebhas.creativesaas.approval.cache;

import com.lebhas.creativesaas.approval.domain.ApprovalStatus;
import com.lebhas.creativesaas.approval.domain.ApprovalWorkflow;

import java.time.Instant;
import java.util.UUID;

public record ReviewerAssignmentCacheEntry(
        UUID workflowId,
        UUID workspaceId,
        UUID generatedVersionId,
        UUID reviewerId,
        ApprovalStatus currentStatus,
        Instant assignedAt,
        Instant cachedAt
) {

    public static ReviewerAssignmentCacheEntry from(ApprovalWorkflow workflow, Instant cachedAt) {
        return new ReviewerAssignmentCacheEntry(
                workflow.getId(),
                workflow.getWorkspaceId(),
                workflow.getGeneratedVersionId(),
                workflow.getCurrentReviewerId(),
                workflow.getCurrentStatus(),
                workflow.getUpdatedAt(),
                cachedAt);
    }
}
