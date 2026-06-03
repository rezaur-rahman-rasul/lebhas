package com.lebhas.creativesaas.prompt.application;

import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.infrastructure.persistence.AssetRepository;
import com.lebhas.creativesaas.brand.domain.BrandEntity;
import com.lebhas.creativesaas.brand.domain.BrandLanguagePreference;
import com.lebhas.creativesaas.brand.infrastructure.persistence.BrandRepository;
import com.lebhas.creativesaas.campaign.domain.ProjectCampaignEntity;
import com.lebhas.creativesaas.campaign.infrastructure.persistence.ProjectCampaignRepository;
import com.lebhas.creativesaas.common.api.ApiError;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.messaging.kafka.BaseDomainEvent;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.pricing.application.WorkspacePlanContextService;
import com.lebhas.creativesaas.pricing.application.dto.PlanFeaturePolicyView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspacePlanContextView;
import com.lebhas.creativesaas.product.domain.ProductServiceEntity;
import com.lebhas.creativesaas.product.infrastructure.persistence.ProductServiceRepository;
import com.lebhas.creativesaas.prompt.application.dto.PromptBuilderContextView;
import com.lebhas.creativesaas.prompt.application.dto.PromptReadinessView;
import com.lebhas.creativesaas.prompt.application.dto.PromptValidationCommand;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.workspace.infrastructure.persistence.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PromptReadinessService {

    public static final int MAX_PROMPT_LENGTH = 5000;

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final WorkspaceRepository workspaceRepository;
    private final BrandRepository brandRepository;
    private final ProductServiceRepository productServiceRepository;
    private final ProjectCampaignRepository projectCampaignRepository;
    private final AssetRepository assetRepository;
    private final WorkspacePlanContextService workspacePlanContextService;
    private final DomainEventPublisher domainEventPublisher;

    public PromptReadinessService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            WorkspaceRepository workspaceRepository,
            BrandRepository brandRepository,
            ProductServiceRepository productServiceRepository,
            ProjectCampaignRepository projectCampaignRepository,
            AssetRepository assetRepository,
            WorkspacePlanContextService workspacePlanContextService,
            DomainEventPublisher domainEventPublisher
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.workspaceRepository = workspaceRepository;
        this.brandRepository = brandRepository;
        this.productServiceRepository = productServiceRepository;
        this.projectCampaignRepository = projectCampaignRepository;
        this.assetRepository = assetRepository;
        this.workspacePlanContextService = workspacePlanContextService;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Transactional(readOnly = true)
    public PromptBuilderContextView getContext(UUID workspaceId, UUID projectId) {
        workspaceAuthorizationService.requirePermission(workspaceId, Permission.PROMPT_INTELLIGENCE_USE);
        ResolvedHierarchy hierarchy = resolveHierarchy(workspaceId, projectId, false);
        PromptReadinessView readiness = validate(new PromptValidationCommand(
                workspaceId,
                projectId,
                null,
                null,
                List.of(),
                false,
                false,
                false));
        return new PromptBuilderContextView(
                workspaceId,
                hierarchy.project() == null ? null : new PromptBuilderContextView.ProjectContext(
                        hierarchy.project().getId(),
                        hierarchy.project().getName(),
                        hierarchy.project().getProductServiceId(),
                        hierarchy.project().getBrandId()),
                hierarchy.product() == null ? null : new PromptBuilderContextView.ProductContext(
                        hierarchy.product().getId(),
                        hierarchy.product().getName(),
                        hierarchy.product().getBrandId(),
                        hierarchy.product().getCategory(),
                        hierarchy.product().getTargetAudience()),
                hierarchy.brand() == null ? null : new PromptBuilderContextView.BrandContext(
                        hierarchy.brand().getId(),
                        hierarchy.brand().getName(),
                        toPromptLanguage(hierarchy.brand().getLanguagePreference()),
                        hierarchy.brand().getBusinessType(),
                        hierarchy.brand().getBrandVoice()),
                readiness);
    }

    @Transactional(readOnly = true)
    public PromptReadinessView validate(PromptValidationCommand command) {
        workspaceAuthorizationService.requirePermission(command.workspaceId(), Permission.PROMPT_INTELLIGENCE_USE);
        List<String> blockingReasons = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        workspaceRepository.findByIdAndDeletedFalse(command.workspaceId())
                .orElseGet(() -> {
                    blockingReasons.add("Workspace does not exist.");
                    return null;
                });

        ResolvedHierarchy hierarchy = resolveHierarchy(command.workspaceId(), command.projectId(), true);
        if (hierarchy.project() == null) {
            blockingReasons.add("Project does not exist in this workspace.");
        }
        if (hierarchy.product() == null) {
            blockingReasons.add("Project is not linked to a product/service in this workspace.");
        }
        if (hierarchy.brand() == null) {
            blockingReasons.add("Product/service is not linked to a brand in this workspace.");
        }
        if (hierarchy.project() != null && hierarchy.product() != null
                && !hierarchy.project().getProductServiceId().equals(hierarchy.product().getId())) {
            blockingReasons.add("Project product/service relationship is invalid.");
        }
        if (hierarchy.product() != null && hierarchy.brand() != null
                && !hierarchy.product().getBrandId().equals(hierarchy.brand().getId())) {
            blockingReasons.add("Product/service brand relationship is invalid.");
        }
        if (hierarchy.brand() != null && command.language() != null
                && !brandAllowsLanguage(hierarchy.brand().getLanguagePreference(), command.language())) {
            blockingReasons.add("Brand language preference does not allow the requested language.");
        }
        if (StringUtils.hasText(command.promptText()) && command.promptText().trim().length() > MAX_PROMPT_LENGTH) {
            blockingReasons.add("Prompt length exceeds the active policy limit.");
        }
        validateAssets(command.workspaceId(), command.projectId(), command.assetIds(), blockingReasons);
        validatePackagePolicy(command, blockingReasons);

        if (!StringUtils.hasText(command.promptText())) {
            warnings.add("Prompt text is empty; enhancement can still use workspace context but generation will need a prompt.");
        }
        return new PromptReadinessView(blockingReasons.isEmpty(), List.copyOf(blockingReasons), List.copyOf(warnings));
    }

    public void assertReady(PromptValidationCommand command) {
        PromptReadinessView readiness = validate(command);
        if (!readiness.ready()) {
            Map<String, Object> payload = new LinkedHashMap<>();
            if (command.projectId() != null) {
                payload.put("projectId", command.projectId());
            }
            payload.put("blockingReasons", readiness.blockingReasons());
            domainEventPublisher.publish(KafkaTopicConstants.PROMPT_VALIDATION_FAILED, new BaseDomainEvent(
                    KafkaTopicConstants.PROMPT_VALIDATION_FAILED,
                    command.workspaceId(),
                    command.projectId(),
                    Instant.now(),
                    payload));
            throw new BusinessException(
                    ErrorCode.PROMPT_READINESS_BLOCKED,
                    ErrorCode.PROMPT_READINESS_BLOCKED.defaultMessage(),
                    readiness.blockingReasons().stream()
                            .map(reason -> ApiError.of(ErrorCode.PROMPT_READINESS_BLOCKED.code(), reason))
                            .toList());
        }
    }

    private ResolvedHierarchy resolveHierarchy(UUID workspaceId, UUID projectId, boolean swallowMissing) {
        ProjectCampaignEntity project = projectCampaignRepository.findByIdAndWorkspaceIdAndDeletedFalse(projectId, workspaceId)
                .orElse(null);
        if (project == null) {
            return new ResolvedHierarchy(null, null, null);
        }
        ProductServiceEntity product = productServiceRepository.findByIdAndWorkspaceIdAndDeletedFalse(project.getProductServiceId(), workspaceId)
                .orElse(null);
        BrandEntity brand = product == null
                ? null
                : brandRepository.findByIdAndWorkspaceIdAndDeletedFalse(product.getBrandId(), workspaceId).orElse(null);
        return new ResolvedHierarchy(project, product, brand);
    }

    private void validateAssets(UUID workspaceId, UUID projectId, List<UUID> assetIds, List<String> blockingReasons) {
        if (assetIds == null || assetIds.isEmpty()) {
            return;
        }
        for (UUID assetId : assetIds) {
            AssetEntity asset = assetRepository.findByIdAndWorkspaceIdAndDeletedFalse(assetId, workspaceId).orElse(null);
            if (asset == null || (projectId != null && !projectId.equals(asset.getProjectId()))) {
                blockingReasons.add("Asset %s does not belong to this project/workspace.".formatted(assetId));
            }
        }
    }

    private void validatePackagePolicy(PromptValidationCommand command, List<String> blockingReasons) {
        WorkspacePlanContextView planContext = workspacePlanContextService.getWorkspacePlanContext(command.workspaceId());
        PlanFeaturePolicyView policy = planContext.featurePolicy();
        if (policy == null) {
            return;
        }
        if (command.requireEnhancement() && !policy.promptEnhancementEnabled()) {
            blockingReasons.add("Active package does not allow prompt enhancement.");
        }
        if ((command.requireSuggestions() || command.requireTemplates()) && !policy.allowAdvancedPromptIntelligence()) {
            blockingReasons.add("Active package does not allow advanced prompt intelligence.");
        }
    }

    private boolean brandAllowsLanguage(BrandLanguagePreference preference, PromptLanguage language) {
        if (preference == null || preference == BrandLanguagePreference.BOTH || language == PromptLanguage.MIXED) {
            return true;
        }
        return (preference == BrandLanguagePreference.ENGLISH && language == PromptLanguage.ENGLISH)
                || (preference == BrandLanguagePreference.BANGLA && language == PromptLanguage.BANGLA);
    }

    private PromptLanguage toPromptLanguage(BrandLanguagePreference preference) {
        if (preference == BrandLanguagePreference.ENGLISH) {
            return PromptLanguage.ENGLISH;
        }
        if (preference == BrandLanguagePreference.BANGLA) {
            return PromptLanguage.BANGLA;
        }
        return PromptLanguage.MIXED;
    }

    private record ResolvedHierarchy(ProjectCampaignEntity project, ProductServiceEntity product, BrandEntity brand) {
    }
}
