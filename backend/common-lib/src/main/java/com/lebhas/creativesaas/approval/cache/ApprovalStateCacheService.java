package com.lebhas.creativesaas.approval.cache;

import com.lebhas.creativesaas.approval.domain.ApprovalWorkflow;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class ApprovalStateCacheService {

    private final ApprovalRedisKeys approvalRedisKeys;
    private final ApprovalRedisAccessSupport approvalRedisAccessSupport;
    private final ApprovalRedisTtlStrategy approvalRedisTtlStrategy;

    public ApprovalStateCacheService(
            ApprovalRedisKeys approvalRedisKeys,
            ApprovalRedisAccessSupport approvalRedisAccessSupport,
            ApprovalRedisTtlStrategy approvalRedisTtlStrategy
    ) {
        this.approvalRedisKeys = approvalRedisKeys;
        this.approvalRedisAccessSupport = approvalRedisAccessSupport;
        this.approvalRedisTtlStrategy = approvalRedisTtlStrategy;
    }

    public Optional<ApprovalStateCacheEntry> getState(UUID generatedVersionId) {
        return approvalRedisAccessSupport.read(
                approvalRedisKeys.approvalState(generatedVersionId),
                ApprovalStateCacheEntry.class,
                "approval_state_get",
                ApprovalRedisOperationContext.workflow(null, null, generatedVersionId, null));
    }

    public boolean cacheState(ApprovalWorkflow workflow) {
        if (workflow == null || workflow.getGeneratedVersionId() == null) {
            return false;
        }
        return cacheState(ApprovalStateCacheEntry.from(workflow));
    }

    public boolean cacheState(ApprovalStateCacheEntry entry) {
        if (entry == null || entry.generatedVersionId() == null) {
            return false;
        }
        return approvalRedisAccessSupport.write(
                approvalRedisKeys.approvalState(entry.generatedVersionId()),
                entry,
                approvalRedisTtlStrategy.approvalStateTtl(),
                "approval_state_put",
                ApprovalRedisOperationContext.workflow(
                        entry.workspaceId(),
                        entry.workflowId(),
                        entry.generatedVersionId(),
                        entry.currentReviewerId()));
    }

    public boolean invalidateState(ApprovalWorkflow workflow) {
        if (workflow == null || workflow.getGeneratedVersionId() == null) {
            return false;
        }
        return invalidateState(
                workflow.getWorkspaceId(),
                workflow.getId(),
                workflow.getGeneratedVersionId(),
                workflow.getCurrentReviewerId());
    }

    public boolean invalidateState(ApprovalStateCacheEntry entry) {
        if (entry == null || entry.generatedVersionId() == null) {
            return false;
        }
        return invalidateState(
                entry.workspaceId(),
                entry.workflowId(),
                entry.generatedVersionId(),
                entry.currentReviewerId());
    }

    public boolean invalidateState(UUID workspaceId, UUID workflowId, UUID generatedVersionId, UUID reviewerId) {
        if (generatedVersionId == null) {
            return false;
        }
        return approvalRedisAccessSupport.delete(
                approvalRedisKeys.approvalState(generatedVersionId),
                "approval_state_delete",
                ApprovalRedisOperationContext.workflow(workspaceId, workflowId, generatedVersionId, reviewerId));
    }
}
