package com.lebhas.creativesaas.approval.cache;

import com.lebhas.creativesaas.approval.domain.ApprovalWorkflow;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class ApprovalWorkflowCacheService {

    private final ApprovalRedisKeys approvalRedisKeys;
    private final ApprovalRedisAccessSupport approvalRedisAccessSupport;
    private final ApprovalRedisTtlStrategy approvalRedisTtlStrategy;

    public ApprovalWorkflowCacheService(
            ApprovalRedisKeys approvalRedisKeys,
            ApprovalRedisAccessSupport approvalRedisAccessSupport,
            ApprovalRedisTtlStrategy approvalRedisTtlStrategy
    ) {
        this.approvalRedisKeys = approvalRedisKeys;
        this.approvalRedisAccessSupport = approvalRedisAccessSupport;
        this.approvalRedisTtlStrategy = approvalRedisTtlStrategy;
    }

    public Optional<ApprovalWorkflowCacheEntry> getWorkflow(UUID workflowId) {
        return approvalRedisAccessSupport.read(
                approvalRedisKeys.approvalWorkflow(workflowId),
                ApprovalWorkflowCacheEntry.class,
                "approval_workflow_get",
                ApprovalRedisOperationContext.workflow(null, workflowId, null, null));
    }

    public boolean cacheWorkflow(ApprovalWorkflow workflow) {
        if (workflow == null || workflow.getId() == null) {
            return false;
        }
        return cacheWorkflow(ApprovalWorkflowCacheEntry.from(workflow));
    }

    public boolean cacheWorkflow(ApprovalWorkflowCacheEntry entry) {
        if (entry == null || entry.workflowId() == null) {
            return false;
        }
        return approvalRedisAccessSupport.write(
                approvalRedisKeys.approvalWorkflow(entry.workflowId()),
                entry,
                approvalRedisTtlStrategy.approvalWorkflowTtl(),
                "approval_workflow_put",
                ApprovalRedisOperationContext.workflow(
                        entry.workspaceId(),
                        entry.workflowId(),
                        entry.generatedVersionId(),
                        entry.currentReviewerId()));
    }

    public boolean invalidateWorkflow(ApprovalWorkflow workflow) {
        if (workflow == null || workflow.getId() == null) {
            return false;
        }
        return invalidateWorkflow(
                workflow.getWorkspaceId(),
                workflow.getId(),
                workflow.getGeneratedVersionId(),
                workflow.getCurrentReviewerId());
    }

    public boolean invalidateWorkflow(ApprovalWorkflowCacheEntry entry) {
        if (entry == null || entry.workflowId() == null) {
            return false;
        }
        return invalidateWorkflow(
                entry.workspaceId(),
                entry.workflowId(),
                entry.generatedVersionId(),
                entry.currentReviewerId());
    }

    public boolean invalidateWorkflow(UUID workspaceId, UUID workflowId, UUID generatedVersionId, UUID reviewerId) {
        if (workflowId == null) {
            return false;
        }
        return approvalRedisAccessSupport.delete(
                approvalRedisKeys.approvalWorkflow(workflowId),
                "approval_workflow_delete",
                ApprovalRedisOperationContext.workflow(workspaceId, workflowId, generatedVersionId, reviewerId));
    }
}
