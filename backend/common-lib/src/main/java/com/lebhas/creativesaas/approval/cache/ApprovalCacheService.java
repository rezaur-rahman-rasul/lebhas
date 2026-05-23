package com.lebhas.creativesaas.approval.cache;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class ApprovalCacheService {

    private final ApprovalRedisKeys approvalRedisKeys;
    private final ApprovalRedisAccessSupport approvalRedisAccessSupport;
    private final ApprovalRedisTtlStrategy approvalRedisTtlStrategy;

    public ApprovalCacheService(
            ApprovalRedisKeys approvalRedisKeys,
            ApprovalRedisAccessSupport approvalRedisAccessSupport,
            ApprovalRedisTtlStrategy approvalRedisTtlStrategy
    ) {
        this.approvalRedisKeys = approvalRedisKeys;
        this.approvalRedisAccessSupport = approvalRedisAccessSupport;
        this.approvalRedisTtlStrategy = approvalRedisTtlStrategy;
    }

    public Optional<ApprovalRequestCacheEntry> getApprovalRequest(UUID approvalRequestId) {
        return approvalRedisAccessSupport.read(
                approvalRedisKeys.approvalRequest(approvalRequestId),
                ApprovalRequestCacheEntry.class,
                "approval_request_get",
                ApprovalRedisOperationContext.of(null, approvalRequestId, null, null));
    }

    public boolean cacheApprovalRequest(ApprovalRequestCacheEntry entry) {
        return approvalRedisAccessSupport.write(
                approvalRedisKeys.approvalRequest(entry.approvalRequestId()),
                entry,
                approvalRedisTtlStrategy.approvalRequestTtl(),
                "approval_request_put",
                ApprovalRedisOperationContext.of(entry.workspaceId(), entry.approvalRequestId(), entry.generatedVersionId(), entry.assignedReviewerId()));
    }

    public boolean invalidateApprovalRequest(UUID workspaceId, UUID approvalRequestId, UUID generatedVersionId, UUID reviewerId) {
        return approvalRedisAccessSupport.delete(
                approvalRedisKeys.approvalRequest(approvalRequestId),
                "approval_request_delete",
                ApprovalRedisOperationContext.of(workspaceId, approvalRequestId, generatedVersionId, reviewerId));
    }

    public Optional<ApprovalStatusCacheEntry> getApprovalStatus(UUID generatedVersionId) {
        return approvalRedisAccessSupport.read(
                approvalRedisKeys.approvalStatus(generatedVersionId),
                ApprovalStatusCacheEntry.class,
                "approval_status_get",
                ApprovalRedisOperationContext.of(null, null, generatedVersionId, null));
    }

    public boolean cacheApprovalStatus(ApprovalStatusCacheEntry entry) {
        return approvalRedisAccessSupport.write(
                approvalRedisKeys.approvalStatus(entry.generatedVersionId()),
                entry,
                approvalRedisTtlStrategy.approvalStatusTtl(),
                "approval_status_put",
                ApprovalRedisOperationContext.of(entry.workspaceId(), entry.approvalRequestId(), entry.generatedVersionId(), entry.latestReviewerId()));
    }

    public boolean invalidateApprovalStatus(UUID workspaceId, UUID approvalRequestId, UUID generatedVersionId, UUID reviewerId) {
        return approvalRedisAccessSupport.delete(
                approvalRedisKeys.approvalStatus(generatedVersionId),
                "approval_status_delete",
                ApprovalRedisOperationContext.of(workspaceId, approvalRequestId, generatedVersionId, reviewerId));
    }

    public void invalidateApproval(UUID workspaceId, UUID approvalRequestId, UUID generatedVersionId, UUID reviewerId) {
        invalidateApprovalRequest(workspaceId, approvalRequestId, generatedVersionId, reviewerId);
        invalidateApprovalStatus(workspaceId, approvalRequestId, generatedVersionId, reviewerId);
    }
}
