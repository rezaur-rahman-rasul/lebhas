package com.lebhas.creativesaas.approval.cache;

import java.util.UUID;

public record ApprovalRedisOperationContext(
        UUID workspaceId,
        UUID approvalRequestId,
        UUID approvalWorkflowId,
        UUID generatedVersionId,
        UUID reviewerId,
        String shareToken
) {

    public static ApprovalRedisOperationContext of(
            UUID workspaceId,
            UUID approvalRequestId,
            UUID generatedVersionId,
            UUID reviewerId
    ) {
        return new ApprovalRedisOperationContext(workspaceId, approvalRequestId, null, generatedVersionId, reviewerId, null);
    }

    public static ApprovalRedisOperationContext workflow(
            UUID workspaceId,
            UUID approvalWorkflowId,
            UUID generatedVersionId,
            UUID reviewerId
    ) {
        return new ApprovalRedisOperationContext(workspaceId, null, approvalWorkflowId, generatedVersionId, reviewerId, null);
    }

    public static ApprovalRedisOperationContext shareLink(
            UUID workspaceId,
            UUID generatedVersionId,
            String shareToken
    ) {
        return new ApprovalRedisOperationContext(workspaceId, null, null, generatedVersionId, null, shareToken);
    }
}
