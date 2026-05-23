package com.lebhas.creativesaas.approval.cache;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class ApprovalReviewerCacheService {

    private final ApprovalRedisKeys approvalRedisKeys;
    private final ApprovalRedisAccessSupport approvalRedisAccessSupport;
    private final ApprovalRedisTtlStrategy approvalRedisTtlStrategy;

    public ApprovalReviewerCacheService(
            ApprovalRedisKeys approvalRedisKeys,
            ApprovalRedisAccessSupport approvalRedisAccessSupport,
            ApprovalRedisTtlStrategy approvalRedisTtlStrategy
    ) {
        this.approvalRedisKeys = approvalRedisKeys;
        this.approvalRedisAccessSupport = approvalRedisAccessSupport;
        this.approvalRedisTtlStrategy = approvalRedisTtlStrategy;
    }

    public Optional<ApprovalReviewerCacheEntry> getReviewerQueue(UUID reviewerId) {
        return approvalRedisAccessSupport.read(
                approvalRedisKeys.approvalReviewer(reviewerId),
                ApprovalReviewerCacheEntry.class,
                "approval_reviewer_get",
                ApprovalRedisOperationContext.of(null, null, null, reviewerId));
    }

    public boolean cacheReviewerQueue(ApprovalReviewerCacheEntry entry) {
        return approvalRedisAccessSupport.write(
                approvalRedisKeys.approvalReviewer(entry.reviewerId()),
                entry,
                approvalRedisTtlStrategy.approvalReviewerTtl(),
                "approval_reviewer_put",
                ApprovalRedisOperationContext.of(null, null, null, entry.reviewerId()));
    }

    public boolean invalidateReviewerQueue(UUID reviewerId) {
        return approvalRedisAccessSupport.delete(
                approvalRedisKeys.approvalReviewer(reviewerId),
                "approval_reviewer_delete",
                ApprovalRedisOperationContext.of(null, null, null, reviewerId));
    }
}
