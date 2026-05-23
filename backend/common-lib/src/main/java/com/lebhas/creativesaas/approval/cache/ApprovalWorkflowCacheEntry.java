package com.lebhas.creativesaas.approval.cache;

import com.lebhas.creativesaas.approval.domain.ApprovalStatus;
import com.lebhas.creativesaas.approval.domain.ApprovalWorkflow;

import java.time.Instant;
import java.util.UUID;

public record ApprovalWorkflowCacheEntry(
        UUID workflowId,
        UUID workspaceId,
        UUID creativeRequestId,
        UUID generatedVersionId,
        UUID createdBy,
        ApprovalStatus currentStatus,
        UUID currentReviewerId,
        Instant createdAt,
        Instant updatedAt
) {

    public static ApprovalWorkflowCacheEntry from(ApprovalWorkflow workflow) {
        return new ApprovalWorkflowCacheEntry(
                workflow.getId(),
                workflow.getWorkspaceId(),
                workflow.getCreativeRequestId(),
                workflow.getGeneratedVersionId(),
                workflow.getCreatedBy(),
                workflow.getCurrentStatus(),
                workflow.getCurrentReviewerId(),
                workflow.getCreatedAt(),
                workflow.getUpdatedAt());
    }
}
