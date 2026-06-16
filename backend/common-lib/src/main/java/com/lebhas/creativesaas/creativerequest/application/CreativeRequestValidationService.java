package com.lebhas.creativesaas.creativerequest.application;

import com.lebhas.ai.cache.ActivePipelineCacheEntry;
import com.lebhas.ai.cache.AiPipelineCacheService;
import com.lebhas.ai.domain.CreativePipeline;
import com.lebhas.ai.domain.CreativePipelineStatus;
import com.lebhas.ai.dto.AiGenerationRequest;
import com.lebhas.ai.infrastructure.persistence.CreativePipelineRepository;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.infrastructure.persistence.AssetRepository;
import com.lebhas.creativesaas.brand.domain.BrandEntity;
import com.lebhas.creativesaas.brand.domain.BrandLanguagePreference;
import com.lebhas.creativesaas.campaign.domain.ProjectCampaignEntity;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.creativerequest.application.dto.CreateCreativeRequestCommand;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.generation.application.CreditEstimationService;
import com.lebhas.creativesaas.generation.domain.CreativeOutputFormat;
import com.lebhas.creativesaas.generation.domain.CreativeType;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.product.domain.ProductServiceEntity;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class CreativeRequestValidationService {

    private static final int MAX_PROMPT_LENGTH = 32_000;

    private final AssetRepository assetRepository;
    private final CreditEstimationService creditEstimationService;
    private final CreativePlanValidationService creativePlanValidationService;
    private final BrandLanguagePromptPolicyService brandLanguagePromptPolicyService;
    private final CreativeHierarchyValidationService creativeHierarchyValidationService;
    private final CreativeRequestPromptFlowService creativeRequestPromptFlowService;
    private final CreativePipelineRepository creativePipelineRepository;
    private final AiPipelineCacheService aiPipelineCacheService;

    public CreativeRequestValidationService(
            AssetRepository assetRepository,
            CreditEstimationService creditEstimationService,
            CreativePlanValidationService creativePlanValidationService,
            BrandLanguagePromptPolicyService brandLanguagePromptPolicyService,
            CreativeHierarchyValidationService creativeHierarchyValidationService,
            CreativeRequestPromptFlowService creativeRequestPromptFlowService,
            CreativePipelineRepository creativePipelineRepository,
            AiPipelineCacheService aiPipelineCacheService
    ) {
        this.assetRepository = assetRepository;
        this.creditEstimationService = creditEstimationService;
        this.creativePlanValidationService = creativePlanValidationService;
        this.brandLanguagePromptPolicyService = brandLanguagePromptPolicyService;
        this.creativeHierarchyValidationService = creativeHierarchyValidationService;
        this.creativeRequestPromptFlowService = creativeRequestPromptFlowService;
        this.creativePipelineRepository = creativePipelineRepository;
        this.aiPipelineCacheService = aiPipelineCacheService;
    }

    public CreativeRequestGenerationPlan validateForCreate(
            CreateCreativeRequestCommand command,
            WorkspaceAuthorizationService.WorkspaceAccess access
    ) {
        if (command.workspaceId() == null) {
            throw new BusinessException(ErrorCode.WORKSPACE_CONTEXT_REQUIRED);
        }
        if (command.projectCampaignId() == null) {
            throw new BusinessException(ErrorCode.PROJECT_CAMPAIGN_NOT_FOUND, "projectCampaignId is required");
        }

        CreativeHierarchyValidationService.CreativeHierarchyContext hierarchy = creativeHierarchyValidationService.validate(
                command.workspaceId(),
                command);
        BrandEntity brand = hierarchy.brand();
        ProductServiceEntity productService = hierarchy.productService();
        ProjectCampaignEntity projectCampaign = hierarchy.projectCampaign();

        List<UUID> selectedAssetIds = normalizeAssetIds(command.selectedAssetIds());
        validateAssets(command.workspaceId(), command.projectCampaignId(), selectedAssetIds);

        String requestName = normalizeRequired(command.requestName(), "requestName");
        String sourcePrompt = normalizeRequired(command.sourcePrompt(), "sourcePrompt");
        validatePrompt(sourcePrompt, command.enhancedPrompt());

        String creativeObjectiveText = normalizeRequired(command.creativeObjective(), "creativeObjective");
        String targetPlatformText = normalizeRequired(command.targetPlatform(), "targetPlatform");
        String requestedFormatText = normalizeRequired(command.requestedFormat(), "requestedFormat");

        CampaignObjective creativeObjective = parseEnum(
                creativeObjectiveText,
                CampaignObjective.class,
                "creativeObjective");
        PromptPlatform platform = parseEnum(
                targetPlatformText,
                PromptPlatform.class,
                "targetPlatform");
        CreativeOutputFormat outputFormat = parseEnum(
                requestedFormatText,
                CreativeOutputFormat.class,
                "requestedFormat");
        CreativeType creativeType = inferCreativeType(outputFormat);
        int requestedVersions = normalizeRequestedVersions(command.requestedVersions());
        BrandLanguagePromptPolicyService.BrandLanguagePromptPolicy languagePolicy = brandLanguagePromptPolicyService.resolve(
                brand.getLanguagePreference(),
                command.languagePreference());
        PromptLanguage promptLanguage = languagePolicy.promptLanguage();
        String enhancedPrompt = creativeRequestPromptFlowService.resolveEnhancedPrompt(
                command.workspaceId(),
                brand,
                productService,
                projectCampaign,
                sourcePrompt,
                promptLanguage,
                platform,
                creativeObjective,
                selectedAssetIds,
                normalizeNullable(command.enhancedPrompt()));
        validatePrompt(sourcePrompt, enhancedPrompt);
        BigDecimal estimatedCreditCost = creditEstimationService.estimate(creativeType, requestedVersions);

        creativePlanValidationService.validateForCreativeRequest(
                command.workspaceId(),
                requestedVersions,
                creativeType,
                estimatedCreditCost);

        AiGenerationRequest requestKey = new AiGenerationRequest(
                command.workspaceId(),
                null,
                null,
                creativeType,
                platform,
                creativeObjective,
                outputFormat,
                promptLanguage,
                enhancedPrompt == null ? sourcePrompt : enhancedPrompt,
                null,
                assetSnapshot(selectedAssetIds),
                duplicateHashConfig(projectCampaign.getId(), requestedVersions, selectedAssetIds),
                null,
                null,
                null);

        UUID activePipelineId = requireActivePipelineId();

        return new CreativeRequestGenerationPlan(
                activePipelineId,
                brand.getId(),
                productService.getId(),
                command.projectCampaignId(),
                requestName,
                sourcePrompt,
                enhancedPrompt,
                languagePolicy.requestLanguagePreference(),
                normalizeNullable(brand.getBrandVoice()),
                normalizeNullable(productService.getTargetAudience()) == null
                        ? normalizeNullable(brand.getTargetAudience())
                        : normalizeNullable(productService.getTargetAudience()),
                normalizeNullable(brand.getPreferredCta()),
                creativeObjectiveText,
                targetPlatformText,
                requestedFormatText,
                requestedVersions,
                selectedAssetIds,
                platform,
                creativeObjective,
                outputFormat,
                creativeType,
                promptLanguage,
                estimatedCreditCost,
                sha256(requestKey.deterministicKey()));
    }

    private UUID requireActivePipelineId() {
        return aiPipelineCacheService.getActivePipeline()
                .map(ActivePipelineCacheEntry::pipelineId)
                .orElseGet(() -> {
                    CreativePipeline pipeline = creativePipelineRepository
                            .findFirstByActiveTrueAndStatusAndDeletedFalse(CreativePipelineStatus.ACTIVE)
                            .orElseThrow(() -> new BusinessException(
                                    ErrorCode.BUSINESS_RULE_VIOLATION,
                                    "An active creative pipeline is required before creating a creative request"));
                    aiPipelineCacheService.storeActivePipeline(new ActivePipelineCacheEntry(
                            pipeline.getId(),
                            pipeline.getPipelineCode(),
                            pipeline.getVersion(),
                            null));
                    return pipeline.getId();
                });
    }

    public CreativeRequestGenerationPlan validateForRetry(
            CreativeRequestEntity request,
            WorkspaceAuthorizationService.WorkspaceAccess access
    ) {
        return validateForCreate(new CreateCreativeRequestCommand(
                request.getWorkspaceId(),
                request.getBrandId(),
                request.getProductServiceId(),
                request.getProjectCampaignId(),
                request.getRequestName(),
                request.getSourcePrompt(),
                request.getEnhancedPrompt(),
                request.getLanguagePreference(),
                request.getCreativeObjective(),
                request.getTargetPlatform(),
                request.getRequestedFormat(),
                request.getRequestedVersions(),
                request.getSelectedAssetIds()),
                access);
    }

    public BigDecimal estimateCost(CreativeRequestEntity request) {
        return estimateCost(request.getRequestedFormat(), request.getRequestedVersions());
    }

    public BigDecimal estimateCost(String requestedFormat, int requestedVersions) {
        CreativeOutputFormat outputFormat = parseEnum(requestedFormat, CreativeOutputFormat.class, "requestedFormat");
        return creditEstimationService.estimate(inferCreativeType(outputFormat))
                .multiply(BigDecimal.valueOf(Math.max(requestedVersions, 1)));
    }

    private void validateAssets(UUID workspaceId, UUID projectCampaignId, List<UUID> selectedAssetIds) {
        for (UUID assetId : selectedAssetIds) {
            AssetEntity asset = assetRepository.findByIdAndWorkspaceIdAndDeletedFalse(assetId, workspaceId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ASSET_NOT_FOUND));
            if (!projectCampaignId.equals(asset.getProjectCampaignId())) {
                throw new BusinessException(
                        ErrorCode.WORKSPACE_ACCESS_DENIED,
                        "Selected assets must belong to the same project campaign");
            }
        }
    }

    private void validatePrompt(String sourcePrompt, String enhancedPrompt) {
        String effectivePrompt = enhancedPrompt == null || enhancedPrompt.isBlank() ? sourcePrompt : enhancedPrompt;
        if (effectivePrompt == null || effectivePrompt.isBlank()) {
            throw new BusinessException(ErrorCode.PROMPT_LENGTH_INVALID, "sourcePrompt or enhancedPrompt is required");
        }
        if (sourcePrompt.length() > MAX_PROMPT_LENGTH || effectivePrompt.length() > MAX_PROMPT_LENGTH) {
            throw new BusinessException(
                    ErrorCode.PROMPT_LENGTH_INVALID,
                    "Prompt length must be " + MAX_PROMPT_LENGTH + " characters or fewer");
        }
    }

    private CreativeType inferCreativeType(CreativeOutputFormat outputFormat) {
        return outputFormat.isVideo() ? CreativeType.SHORT_VIDEO : CreativeType.STATIC_IMAGE;
    }

    private Map<String, Object> duplicateHashConfig(UUID projectCampaignId, int requestedVersions, List<UUID> selectedAssetIds) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("projectCampaignId", projectCampaignId);
        config.put("requestedVersions", requestedVersions);
        config.put("selectedAssetIds", selectedAssetIds);
        return config;
    }

    private String assetSnapshot(List<UUID> selectedAssetIds) {
        if (selectedAssetIds.isEmpty()) {
            return null;
        }
        return selectedAssetIds.toString();
    }

    private List<UUID> normalizeAssetIds(List<UUID> selectedAssetIds) {
        if (selectedAssetIds == null || selectedAssetIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<UUID> normalized = new LinkedHashSet<>();
        for (UUID assetId : selectedAssetIds) {
            if (assetId != null) {
                normalized.add(assetId);
            }
        }
        return List.copyOf(new ArrayList<>(normalized));
    }

    private String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.GENERATION_VALIDATION_FAILED, field + " is required");
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int normalizeRequestedVersions(Integer requestedVersions) {
        if (requestedVersions == null) {
            return 1;
        }
        if (requestedVersions < 1) {
            throw new BusinessException(ErrorCode.GENERATION_VALIDATION_FAILED, "requestedVersions must be greater than zero");
        }
        return requestedVersions;
    }

    private <E extends Enum<E>> E parseEnum(String value, Class<E> enumType, String field) {
        try {
            return Enum.valueOf(enumType, normalizeEnumValue(value));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.GENERATION_VALIDATION_FAILED, field + " is invalid");
        }
    }

    private String normalizeEnumValue(String value) {
        return value.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }
}
