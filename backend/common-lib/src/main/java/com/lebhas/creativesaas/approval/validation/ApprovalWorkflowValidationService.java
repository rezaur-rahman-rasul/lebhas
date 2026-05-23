package com.lebhas.creativesaas.approval.validation;

import com.lebhas.creativesaas.approval.domain.ApprovalWorkflow;
import com.lebhas.creativesaas.approval.infrastructure.persistence.ApprovalWorkflowRepository;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.exception.TenantIsolationException;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.creativerequest.infrastructure.persistence.CreativeRequestRepository;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.generatedversion.infrastructure.persistence.GeneratedVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ApprovalWorkflowValidationService {

    private final GeneratedVersionRepository generatedVersionRepository;
    private final CreativeRequestRepository creativeRequestRepository;
    private final ApprovalWorkflowRepository approvalWorkflowRepository;
    private final ApprovalPlanValidationService approvalPlanValidationService;

    public ApprovalWorkflowValidationService(
            GeneratedVersionRepository generatedVersionRepository,
            CreativeRequestRepository creativeRequestRepository,
            ApprovalWorkflowRepository approvalWorkflowRepository,
            ApprovalPlanValidationService approvalPlanValidationService
    ) {
        this.generatedVersionRepository = generatedVersionRepository;
        this.creativeRequestRepository = creativeRequestRepository;
        this.approvalWorkflowRepository = approvalWorkflowRepository;
        this.approvalPlanValidationService = approvalPlanValidationService;
    }

    @Transactional(readOnly = true)
    public void requireApprovalWorkflowCreationAllowed(UUID workspaceId, UUID creativeRequestId, UUID generatedVersionId) {
        approvalPlanValidationService.requireApprovalWorkflowEnabled(workspaceId);
        GeneratedVersionEntity generatedVersion = requireGeneratedVersionBelongsToWorkspace(workspaceId, generatedVersionId);
        requireCreativeRequestBelongsToWorkspace(workspaceId, creativeRequestId);
        requireGeneratedVersionMatchesCreativeRequest(generatedVersion, creativeRequestId);
        if (approvalWorkflowRepository.existsByWorkspaceIdAndGeneratedVersionId(workspaceId, generatedVersionId)) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Approval workflow already exists for the generated version");
        }
    }

    @Transactional(readOnly = true)
    public GeneratedVersionEntity requireGeneratedVersionBelongsToWorkspace(UUID workspaceId, UUID generatedVersionId) {
        return generatedVersionRepository.findByIdAndWorkspaceIdAndDeletedFalse(generatedVersionId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GENERATED_VERSION_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public CreativeRequestEntity requireCreativeRequestBelongsToWorkspace(UUID workspaceId, UUID creativeRequestId) {
        return creativeRequestRepository.findByIdAndWorkspaceIdAndDeletedFalse(creativeRequestId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREATIVE_REQUEST_FOUNDATION_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public ApprovalWorkflow requireApprovalWorkflowBelongsToWorkspace(UUID workspaceId, UUID approvalWorkflowId) {
        return approvalWorkflowRepository.findByIdAndWorkspaceId(approvalWorkflowId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREATIVE_APPROVAL_NOT_FOUND, "Approval workflow not found"));
    }

    public void requireGeneratedVersionMatchesCreativeRequest(GeneratedVersionEntity generatedVersion, UUID creativeRequestId) {
        if (generatedVersion == null || creativeRequestId == null || !creativeRequestId.equals(generatedVersion.getCreativeRequestId())) {
            throw new TenantIsolationException("Generated version does not belong to the creative request");
        }
    }

    public void requireApprovalWorkflowMatchesGeneratedVersion(ApprovalWorkflow workflow, GeneratedVersionEntity generatedVersion) {
        if (workflow == null
                || generatedVersion == null
                || !workflow.getGeneratedVersionId().equals(generatedVersion.getId())
                || !workflow.getWorkspaceId().equals(generatedVersion.getWorkspaceId())
                || !workflow.getCreativeRequestId().equals(generatedVersion.getCreativeRequestId())) {
            throw new TenantIsolationException("Approval workflow does not belong to the generated version");
        }
    }
}
