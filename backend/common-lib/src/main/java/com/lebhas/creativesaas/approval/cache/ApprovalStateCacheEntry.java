package com.lebhas.creativesaas.approval.cache;

import com.lebhas.creativesaas.approval.domain.ApprovalStatus;
import com.lebhas.creativesaas.approval.domain.ApprovalWorkflow;

import java.time.Instant;
import java.util.UUID;

public record ApprovalStateCacheEntry(
        UUID generatedVersionId,
        UUID workflowId,
        UUID workspaceId,
        UUID creativeRequestId,
        ApprovalStatus currentStatus,
        UUID currentReviewerId,
        Instant updatedAt
) {

    public static ApprovalStateCacheEntry from(ApprovalWorkflow workflow) {
        return new ApprovalStateCacheEntry(
                workflow.getGeneratedVersionId(),
                workflow.getId(),
                workflow.getWorkspaceId(),
                workflow.getCreativeRequestId(),
                workflow.getCurrentStatus(),
                workflow.getCurrentReviewerId(),
                workflow.getUpdatedAt());
    }
}
