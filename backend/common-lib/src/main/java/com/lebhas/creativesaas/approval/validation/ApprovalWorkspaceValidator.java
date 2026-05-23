package com.lebhas.creativesaas.approval.validation;

import com.lebhas.creativesaas.approval.domain.ApprovalRequest;
import com.lebhas.creativesaas.approval.infrastructure.persistence.ApprovalRequestRepository;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.exception.TenantIsolationException;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.generatedversion.infrastructure.persistence.GeneratedVersionRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ApprovalWorkspaceValidator {

    private final GeneratedVersionRepository generatedVersionRepository;
    private final ApprovalRequestRepository approvalRequestRepository;

    public ApprovalWorkspaceValidator(
            GeneratedVersionRepository generatedVersionRepository,
            ApprovalRequestRepository approvalRequestRepository
    ) {
        this.generatedVersionRepository = generatedVersionRepository;
        this.approvalRequestRepository = approvalRequestRepository;
    }

    public GeneratedVersionEntity requireGeneratedVersionBelongsToWorkspace(UUID workspaceId, UUID generatedVersionId) {
        return generatedVersionRepository.findByIdAndWorkspaceIdAndDeletedFalse(generatedVersionId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GENERATED_VERSION_NOT_FOUND));
    }

    public ApprovalRequest requireApprovalRequestBelongsToWorkspace(UUID workspaceId, UUID approvalRequestId) {
        return approvalRequestRepository.findByIdAndWorkspaceIdAndDeletedFalse(approvalRequestId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREATIVE_APPROVAL_NOT_FOUND, "Approval request not found"));
    }

    public void requireApprovalRequestMatchesGeneratedVersion(ApprovalRequest approvalRequest, GeneratedVersionEntity generatedVersion) {
        if (approvalRequest == null || generatedVersion == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Approval request and generated version are required");
        }
        if (!approvalRequest.getGeneratedVersionId().equals(generatedVersion.getId())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Approval request does not belong to the generated version");
        }
    }

    public void requireSameWorkspace(UUID expectedWorkspaceId, UUID actualWorkspaceId, String resourceLabel) {
        if (expectedWorkspaceId == null || actualWorkspaceId == null || !expectedWorkspaceId.equals(actualWorkspaceId)) {
            throw new TenantIsolationException((resourceLabel == null ? "Resource" : resourceLabel) + " does not belong to the workspace");
        }
    }
}
