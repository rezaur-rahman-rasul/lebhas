package com.lebhas.creativesaas.approval.cache;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class ApprovalCountCacheService {

    private final ApprovalRedisKeys approvalRedisKeys;
    private final ApprovalRedisAccessSupport approvalRedisAccessSupport;
    private final ApprovalRedisTtlStrategy approvalRedisTtlStrategy;

    public ApprovalCountCacheService(
            ApprovalRedisKeys approvalRedisKeys,
            ApprovalRedisAccessSupport approvalRedisAccessSupport,
            ApprovalRedisTtlStrategy approvalRedisTtlStrategy
    ) {
        this.approvalRedisKeys = approvalRedisKeys;
        this.approvalRedisAccessSupport = approvalRedisAccessSupport;
        this.approvalRedisTtlStrategy = approvalRedisTtlStrategy;
    }

    public Optional<ApprovalPendingCacheEntry> getPendingApprovals(UUID workspaceId) {
        return approvalRedisAccessSupport.read(
                approvalRedisKeys.approvalPending(workspaceId),
                ApprovalPendingCacheEntry.class,
                "approval_pending_get",
                ApprovalRedisOperationContext.of(workspaceId, null, null, null));
    }

    public boolean cachePendingApprovals(ApprovalPendingCacheEntry entry) {
        return approvalRedisAccessSupport.write(
                approvalRedisKeys.approvalPending(entry.workspaceId()),
                entry,
                approvalRedisTtlStrategy.approvalPendingTtl(),
                "approval_pending_put",
                ApprovalRedisOperationContext.of(entry.workspaceId(), null, null, null));
    }

    public boolean invalidatePendingApprovals(UUID workspaceId) {
        return approvalRedisAccessSupport.delete(
                approvalRedisKeys.approvalPending(workspaceId),
                "approval_pending_delete",
                ApprovalRedisOperationContext.of(workspaceId, null, null, null));
    }
}
