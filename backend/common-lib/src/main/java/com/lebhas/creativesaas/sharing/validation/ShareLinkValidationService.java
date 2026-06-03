package com.lebhas.creativesaas.sharing.validation;

import com.lebhas.creativesaas.approval.validation.ApprovalPermissionValidationService;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.generatedversion.infrastructure.persistence.GeneratedVersionRepository;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.pricing.application.WorkspacePlanContextService;
import com.lebhas.creativesaas.pricing.application.dto.PlanFeaturePolicyView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspacePlanContextView;
import com.lebhas.creativesaas.sharing.domain.ShareLink;
import com.lebhas.creativesaas.sharing.infrastructure.persistence.ShareLinkRepository;
import com.lebhas.pricing.WorkspaceSubscriptionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class ShareLinkValidationService {

    private final GeneratedVersionRepository generatedVersionRepository;
    private final ShareLinkRepository shareLinkRepository;
    private final WorkspacePlanContextService workspacePlanContextService;
    private final ApprovalPermissionValidationService approvalPermissionValidationService;

    public ShareLinkValidationService(
            GeneratedVersionRepository generatedVersionRepository,
            ShareLinkRepository shareLinkRepository,
            WorkspacePlanContextService workspacePlanContextService,
            ApprovalPermissionValidationService approvalPermissionValidationService
    ) {
        this.generatedVersionRepository = generatedVersionRepository;
        this.shareLinkRepository = shareLinkRepository;
        this.workspacePlanContextService = workspacePlanContextService;
        this.approvalPermissionValidationService = approvalPermissionValidationService;
    }

    @Transactional(readOnly = true)
    public void requireShareLinkCreationAllowed(
            WorkspaceAuthorizationService.WorkspaceAccess access,
            UUID generatedVersionId,
            String tokenHash
    ) {
        if (access == null || access.workspace() == null) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED);
        }
        UUID workspaceId = access.workspace().getId();
        requirePublicShareLinksEnabled(workspaceId);
        approvalPermissionValidationService.requireShareLinkCreation(access);
        GeneratedVersionEntity generatedVersion = requireGeneratedVersionBelongsToWorkspace(workspaceId, generatedVersionId);
        if (generatedVersion.getApprovalStatus() != com.lebhas.creativesaas.generatedversion.domain.ApprovalStatus.APPROVED) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Generated version must be approved before sharing");
        }
        requireTokenHashAvailable(tokenHash);
    }

    @Transactional(readOnly = true)
    public GeneratedVersionEntity requireGeneratedVersionBelongsToWorkspace(UUID workspaceId, UUID generatedVersionId) {
        return generatedVersionRepository.findByIdAndWorkspaceIdAndDeletedFalse(generatedVersionId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GENERATED_VERSION_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public ShareLink requireShareLinkBelongsToWorkspace(UUID workspaceId, UUID shareLinkId) {
        return shareLinkRepository.findByIdAndWorkspaceId(shareLinkId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Share link not found"));
    }

    @Transactional(readOnly = true)
    public ShareLink requireShareLinkToken(String token) {
        return shareLinkRepository.findByTokenHash(normalizeRequired(token, "tokenHash"))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Share link not found"));
    }

    @Transactional(readOnly = true)
    public ShareLink requireShareLinkTokenBelongsToWorkspace(UUID workspaceId, String token) {
        return shareLinkRepository.findByTokenHashAndWorkspaceId(normalizeRequired(token, "tokenHash"), workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Share link not found"));
    }

    @Transactional(readOnly = true)
    public void requireTokenAvailable(String token) {
        if (shareLinkRepository.existsByToken(normalizeRequired(token, "token"))) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Share token is already in use");
        }
    }

    @Transactional(readOnly = true)
    public void requireTokenHashAvailable(String tokenHash) {
        if (shareLinkRepository.existsByTokenHash(normalizeRequired(tokenHash, "tokenHash"))) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Share token is already in use");
        }
    }

    @Transactional(readOnly = true)
    public void requirePublicShareLinksEnabled(UUID workspaceId) {
        if (!isPublicShareLinksEnabled(workspaceId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Public share links are not enabled for the workspace plan");
        }
    }

    @Transactional(readOnly = true)
    public boolean isPublicShareLinksEnabled(UUID workspaceId) {
        return findActivePlanFeaturePolicy(workspaceId)
                .map(PlanFeaturePolicyView::allowPublicShareLinks)
                .orElse(false);
    }

    private Optional<PlanFeaturePolicyView> findActivePlanFeaturePolicy(UUID workspaceId) {
        if (workspaceId == null) {
            return Optional.empty();
        }
        WorkspacePlanContextView context = workspacePlanContextService.getWorkspacePlanContext(workspaceId);
        if (context.subscription() == null
                || !isUsableSubscriptionStatus(context.subscription().status())
                || context.pricingPlan() == null
                || !context.pricingPlan().active()) {
            return Optional.empty();
        }
        return Optional.ofNullable(context.featurePolicy());
    }

    private boolean isUsableSubscriptionStatus(WorkspaceSubscriptionStatus status) {
        return status == WorkspaceSubscriptionStatus.ACTIVE || status == WorkspaceSubscriptionStatus.TRIAL;
    }

    private static String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, field + " must not be blank");
        }
        return value.trim();
    }
}
