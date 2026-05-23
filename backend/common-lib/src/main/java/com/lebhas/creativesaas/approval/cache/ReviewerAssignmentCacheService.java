package com.lebhas.creativesaas.approval.cache;

import com.lebhas.creativesaas.approval.domain.ApprovalWorkflow;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReviewerAssignmentCacheService {

    private final ApprovalRedisKeys approvalRedisKeys;
    private final ApprovalRedisAccessSupport approvalRedisAccessSupport;
    private final ApprovalRedisTtlStrategy approvalRedisTtlStrategy;
    private final Clock clock;

    public ReviewerAssignmentCacheService(
            ApprovalRedisKeys approvalRedisKeys,
            ApprovalRedisAccessSupport approvalRedisAccessSupport,
            ApprovalRedisTtlStrategy approvalRedisTtlStrategy,
            Clock clock
    ) {
        this.approvalRedisKeys = approvalRedisKeys;
        this.approvalRedisAccessSupport = approvalRedisAccessSupport;
        this.approvalRedisTtlStrategy = approvalRedisTtlStrategy;
        this.clock = clock;
    }

    public Optional<ReviewerAssignmentCacheEntry> getAssignment(UUID workflowId) {
        return approvalRedisAccessSupport.read(
                approvalRedisKeys.reviewerAssignment(workflowId),
                ReviewerAssignmentCacheEntry.class,
                "reviewer_assignment_get",
                ApprovalRedisOperationContext.workflow(null, workflowId, null, null));
    }

    public boolean cacheAssignment(ApprovalWorkflow workflow) {
        if (workflow == null || workflow.getId() == null || workflow.getCurrentReviewerId() == null) {
            return false;
        }
        return cacheAssignment(ReviewerAssignmentCacheEntry.from(workflow, clock.instant()));
    }

    public boolean cacheAssignment(ReviewerAssignmentCacheEntry entry) {
        if (entry == null || entry.workflowId() == null || entry.reviewerId() == null) {
            return false;
        }
        return approvalRedisAccessSupport.write(
                approvalRedisKeys.reviewerAssignment(entry.workflowId()),
                entry,
                approvalRedisTtlStrategy.reviewerAssignmentTtl(),
                "reviewer_assignment_put",
                ApprovalRedisOperationContext.workflow(
                        entry.workspaceId(),
                        entry.workflowId(),
                        entry.generatedVersionId(),
                        entry.reviewerId()));
    }

    public boolean invalidateAssignment(ApprovalWorkflow workflow) {
        if (workflow == null || workflow.getId() == null) {
            return false;
        }
        return invalidateAssignment(
                workflow.getWorkspaceId(),
                workflow.getId(),
                workflow.getGeneratedVersionId(),
                workflow.getCurrentReviewerId());
    }

    public boolean invalidateAssignment(ReviewerAssignmentCacheEntry entry) {
        if (entry == null || entry.workflowId() == null) {
            return false;
        }
        return invalidateAssignment(
                entry.workspaceId(),
                entry.workflowId(),
                entry.generatedVersionId(),
                entry.reviewerId());
    }

    public boolean invalidateAssignment(UUID workspaceId, UUID workflowId, UUID generatedVersionId, UUID reviewerId) {
        if (workflowId == null) {
            return false;
        }
        return approvalRedisAccessSupport.delete(
                approvalRedisKeys.reviewerAssignment(workflowId),
                "reviewer_assignment_delete",
                ApprovalRedisOperationContext.workflow(workspaceId, workflowId, generatedVersionId, reviewerId));
    }
}
