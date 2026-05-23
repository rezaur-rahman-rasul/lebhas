package com.lebhas.creativesaas.usage.application;

import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.generation.domain.CreativeType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class UsagePlanEnforcementService {

    private final QuotaValidationService quotaValidationService;

    public UsagePlanEnforcementService(QuotaValidationService quotaValidationService) {
        this.quotaValidationService = quotaValidationService;
    }

    public void enforceCreativeRequestGeneration(UUID workspaceId, CreativeRequestEntity creativeRequest, BigDecimal serverCalculatedCredits) {
        quotaValidationService.validateVideoGenerationAllowed(workspaceId, creativeRequest.getCreativeType());
        quotaValidationService.validateGeneratedVersionsPerRequest(
                workspaceId,
                creativeRequest.getId(),
                creativeRequest.getRequestedVersions());
        quotaValidationService.validateMonthlyCreditLimit(workspaceId, serverCalculatedCredits);
    }

    public void enforceGeneratedVersionLimit(UUID workspaceId, UUID creativeRequestId, int requestedAdditionalVersions) {
        quotaValidationService.validateGeneratedVersionsPerRequest(workspaceId, creativeRequestId, requestedAdditionalVersions);
    }

    public void enforceMonthlyCreditLimit(UUID workspaceId, BigDecimal serverCalculatedCredits) {
        quotaValidationService.validateMonthlyCreditLimit(workspaceId, serverCalculatedCredits);
    }

    public void enforceStorageLimit(UUID workspaceId, long additionalBytes) {
        quotaValidationService.validateStorageLimit(workspaceId, additionalBytes);
    }

    public void enforcePublicShareLinks(UUID workspaceId) {
        quotaValidationService.validatePublicShareLinksAllowed(workspaceId);
    }

    public void enforceApprovalWorkflow(UUID workspaceId) {
        quotaValidationService.validateApprovalWorkflowAllowed(workspaceId);
    }

    public void enforceVideoGeneration(UUID workspaceId, CreativeType creativeType) {
        quotaValidationService.validateVideoGenerationAllowed(workspaceId, creativeType);
    }

    public void enforceTeamMemberLimit(UUID workspaceId, int additionalMembers) {
        quotaValidationService.validateTeamMemberLimit(workspaceId, additionalMembers);
    }
}
