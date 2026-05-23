package com.lebhas.creativesaas.approval.cache;

import com.lebhas.creativesaas.redis.RedisLockService;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class ApprovalLockService {

    private final ApprovalRedisKeys approvalRedisKeys;
    private final ApprovalRedisAccessSupport approvalRedisAccessSupport;
    private final ApprovalRedisTtlStrategy approvalRedisTtlStrategy;

    public ApprovalLockService(
            ApprovalRedisKeys approvalRedisKeys,
            ApprovalRedisAccessSupport approvalRedisAccessSupport,
            ApprovalRedisTtlStrategy approvalRedisTtlStrategy
    ) {
        this.approvalRedisKeys = approvalRedisKeys;
        this.approvalRedisAccessSupport = approvalRedisAccessSupport;
        this.approvalRedisTtlStrategy = approvalRedisTtlStrategy;
    }

    public Optional<RedisLockService.RedisLockToken> acquireReviewSubmissionLock(
            UUID workspaceId,
            UUID approvalRequestId,
            UUID generatedVersionId,
            UUID reviewerId
    ) {
        return approvalRedisAccessSupport.acquireLock(
                approvalRedisKeys.approvalLock(approvalRequestId),
                approvalRedisTtlStrategy.approvalLockTtl(),
                "approval_review_lock_acquire",
                ApprovalRedisOperationContext.of(workspaceId, approvalRequestId, generatedVersionId, reviewerId));
    }

    public Optional<RedisLockService.RedisLockToken> acquireRevisionLock(
            UUID workspaceId,
            UUID approvalRequestId,
            UUID generatedVersionId,
            UUID reviewerId
    ) {
        return approvalRedisAccessSupport.acquireLock(
                approvalRedisKeys.approvalRevision(generatedVersionId),
                approvalRedisTtlStrategy.approvalRevisionLockTtl(),
                "approval_revision_lock_acquire",
                ApprovalRedisOperationContext.of(workspaceId, approvalRequestId, generatedVersionId, reviewerId));
    }

    public boolean releaseLock(
            RedisLockService.RedisLockToken token,
            UUID workspaceId,
            UUID approvalRequestId,
            UUID generatedVersionId,
            UUID reviewerId
    ) {
        return approvalRedisAccessSupport.releaseLock(
                token,
                "approval_lock_release",
                ApprovalRedisOperationContext.of(workspaceId, approvalRequestId, generatedVersionId, reviewerId));
    }
}
