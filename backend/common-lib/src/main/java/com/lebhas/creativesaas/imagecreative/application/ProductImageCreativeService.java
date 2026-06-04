package com.lebhas.creativesaas.imagecreative.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.ai.application.GeneratedVersionQualityService;
import com.lebhas.ai.application.MasterAiProviderToolRegistryService;
import com.lebhas.ai.application.dto.QualityScoreInput;
import com.lebhas.ai.application.dto.ResolvedProviderRouteView;
import com.lebhas.ai.domain.AiProviderCredential;
import com.lebhas.ai.domain.AiToolProvider;
import com.lebhas.ai.domain.CredentialStatus;
import com.lebhas.ai.domain.CreativeTool;
import com.lebhas.ai.domain.ProviderStatus;
import com.lebhas.ai.domain.ToolCreditCostPolicy;
import com.lebhas.ai.infrastructure.persistence.AiProviderCredentialRepository;
import com.lebhas.ai.infrastructure.persistence.AiToolProviderRepository;
import com.lebhas.ai.infrastructure.persistence.CreativeToolRepository;
import com.lebhas.ai.infrastructure.persistence.ToolCreditCostPolicyRepository;
import com.lebhas.creativesaas.asset.domain.AssetCategory;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.domain.AssetFileType;
import com.lebhas.creativesaas.asset.domain.AssetType;
import com.lebhas.creativesaas.asset.infrastructure.persistence.AssetRepository;
import com.lebhas.creativesaas.asset.storage.StorageService;
import com.lebhas.creativesaas.brand.domain.BrandEntity;
import com.lebhas.creativesaas.brand.domain.BrandLanguagePreference;
import com.lebhas.creativesaas.brand.infrastructure.persistence.BrandRepository;
import com.lebhas.creativesaas.campaign.domain.ProjectCampaignEntity;
import com.lebhas.creativesaas.campaign.infrastructure.persistence.ProjectCampaignRepository;
import com.lebhas.creativesaas.common.api.ApiError;
import com.lebhas.creativesaas.common.api.PagedResult;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestStatus;
import com.lebhas.creativesaas.creativerequest.infrastructure.persistence.CreativeRequestRepository;
import com.lebhas.creativesaas.generatedversion.application.GeneratedVersionViewMapper;
import com.lebhas.creativesaas.generatedversion.application.dto.GeneratedVersionView;
import com.lebhas.creativesaas.generatedversion.domain.ApprovalStatus;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionStatus;
import com.lebhas.creativesaas.generatedversion.domain.GenerationStatus;
import com.lebhas.creativesaas.generatedversion.infrastructure.persistence.GeneratedVersionRepository;
import com.lebhas.creativesaas.generation.application.CreativeCreditReservationService;
import com.lebhas.creativesaas.generation.application.dto.CreditFinalizeCommand;
import com.lebhas.creativesaas.generation.application.dto.CreditRefundCommand;
import com.lebhas.creativesaas.generation.application.dto.CreditReservationResult;
import com.lebhas.creativesaas.generation.domain.CreativeType;
import com.lebhas.creativesaas.imagecreative.application.dto.ImageCreativeCostPreviewView;
import com.lebhas.creativesaas.imagecreative.application.dto.ImageCreativeGenerationView;
import com.lebhas.creativesaas.imagecreative.application.dto.ProductImageCreativeCommand;
import com.lebhas.creativesaas.imagecreative.application.dto.ProductImageCreativeGenerationResult;
import com.lebhas.creativesaas.imagecreative.application.dto.ProductImageCreativeReadinessView;
import com.lebhas.creativesaas.imagecreative.application.dto.ProductImageCreativeRequest;
import com.lebhas.creativesaas.imagecreative.domain.ImageCreativeFormat;
import com.lebhas.creativesaas.imagecreative.domain.ImageCreativeGeneration;
import com.lebhas.creativesaas.imagecreative.domain.ImageCreativeQualityMode;
import com.lebhas.creativesaas.imagecreative.infrastructure.persistence.ImageCreativeGenerationRepository;
import com.lebhas.creativesaas.messaging.kafka.BaseDomainEvent;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.pricing.application.WorkspacePlanContextService;
import com.lebhas.creativesaas.pricing.application.dto.PlanFeaturePolicyView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspacePlanContextView;
import com.lebhas.creativesaas.product.domain.ProductServiceEntity;
import com.lebhas.creativesaas.product.infrastructure.persistence.ProductServiceRepository;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.usage.domain.UsageBillingLog;
import com.lebhas.creativesaas.storage.application.StorageFileService;
import com.lebhas.creativesaas.storage.domain.StorageFileEntity;
import com.lebhas.creativesaas.usage.application.CreditBalanceService;
import com.lebhas.creativesaas.usage.application.dto.CreditBalanceView;
import com.lebhas.creativesaas.usage.infrastructure.persistence.UsageBillingLogRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductImageCreativeService {

    public static final String TOOL_CODE = "CAMPAIGN_CREATIVE_GENERATOR";
    private static final String REFERENCE_TYPE = "product_image_creative_generation";
    private static final UUID SYSTEM_USER_ID = new UUID(0L, 0L);

    private final ProjectCampaignRepository projectCampaignRepository;
    private final BrandRepository brandRepository;
    private final ProductServiceRepository productRepository;
    private final AssetRepository assetRepository;
    private final AiToolProviderRepository providerRepository;
    private final AiProviderCredentialRepository credentialRepository;
    private final CreativeToolRepository toolRepository;
    private final ToolCreditCostPolicyRepository costPolicyRepository;
    private final WorkspacePlanContextService planContextService;
    private final MasterAiProviderToolRegistryService providerToolRegistryService;
    private final CreativeCreditReservationService creditReservationService;
    private final ObjectProvider<ProductImageCreativeProvider> provider;
    private final ImageCreativeGenerationRepository generationRepository;
    private final CreativeRequestRepository creativeRequestRepository;
    private final GeneratedVersionRepository generatedVersionRepository;
    private final GeneratedVersionViewMapper generatedVersionViewMapper;
    private final UsageBillingLogRepository usageBillingLogRepository;
    private final CreditBalanceService creditBalanceService;
    private final StorageFileService storageFileService;
    private final GeneratedVersionQualityService qualityService;
    private final StorageService storageService;
    private final ImageCreativeMapper mapper;
    private final ObjectMapper objectMapper;
    private DomainEventPublisher domainEventPublisher;
    private CurrentUserContext currentUserContext;

    public ProductImageCreativeService(
            ProjectCampaignRepository projectCampaignRepository,
            BrandRepository brandRepository,
            ProductServiceRepository productRepository,
            AssetRepository assetRepository,
            AiToolProviderRepository providerRepository,
            AiProviderCredentialRepository credentialRepository,
            CreativeToolRepository toolRepository,
            ToolCreditCostPolicyRepository costPolicyRepository,
            WorkspacePlanContextService planContextService,
            MasterAiProviderToolRegistryService providerToolRegistryService,
            CreativeCreditReservationService creditReservationService,
            ObjectProvider<ProductImageCreativeProvider> provider,
            ImageCreativeGenerationRepository generationRepository,
            CreativeRequestRepository creativeRequestRepository,
            GeneratedVersionRepository generatedVersionRepository,
            GeneratedVersionViewMapper generatedVersionViewMapper,
            UsageBillingLogRepository usageBillingLogRepository,
            CreditBalanceService creditBalanceService,
            StorageFileService storageFileService,
            GeneratedVersionQualityService qualityService,
            StorageService storageService,
            ImageCreativeMapper mapper,
            ObjectMapper objectMapper
    ) {
        this.projectCampaignRepository = projectCampaignRepository;
        this.brandRepository = brandRepository;
        this.productRepository = productRepository;
        this.assetRepository = assetRepository;
        this.providerRepository = providerRepository;
        this.credentialRepository = credentialRepository;
        this.toolRepository = toolRepository;
        this.costPolicyRepository = costPolicyRepository;
        this.planContextService = planContextService;
        this.providerToolRegistryService = providerToolRegistryService;
        this.creditReservationService = creditReservationService;
        this.provider = provider;
        this.generationRepository = generationRepository;
        this.creativeRequestRepository = creativeRequestRepository;
        this.generatedVersionRepository = generatedVersionRepository;
        this.generatedVersionViewMapper = generatedVersionViewMapper;
        this.usageBillingLogRepository = usageBillingLogRepository;
        this.creditBalanceService = creditBalanceService;
        this.storageFileService = storageFileService;
        this.qualityService = qualityService;
        this.storageService = storageService;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Autowired(required = false)
    void setDomainEventPublisher(DomainEventPublisher domainEventPublisher) {
        this.domainEventPublisher = domainEventPublisher;
    }

    @Autowired(required = false)
    void setCurrentUserContext(CurrentUserContext currentUserContext) {
        this.currentUserContext = currentUserContext;
    }

    @Transactional(readOnly = true)
    public ImageCreativeCostPreviewView previewCost(ProductImageCreativeCommand command) {
        ValidationContext context = validate(command);
        return new ImageCreativeCostPreviewView(
                context.tool().getToolCode(),
                context.qualityMode(),
                context.requestedVersionCount(),
                context.unitCreditCost(),
                context.totalCreditCost());
    }

    @Transactional(readOnly = true)
    public ProductImageCreativeReadinessView readiness(
            UUID workspaceId,
            UUID projectId,
            UUID productAssetId,
            String requestedQualityMode,
            int requestedVersionCount
    ) {
        List<ProductImageCreativeReadinessView.ReadinessMessage> messages = new ArrayList<>();
        boolean workspaceReady = true;
        boolean packageReady = true;
        boolean creditsReady = true;
        boolean providerReady = true;
        boolean routingReady = true;
        boolean productAssetReady = true;

        ProjectCampaignEntity project = null;
        try {
            project = projectCampaignRepository.findByIdAndWorkspaceIdAndDeletedFalse(
                            requireBusiness(projectId, "projectId"),
                            requireBusiness(workspaceId, "workspaceId"))
                    .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_CAMPAIGN_NOT_FOUND, "Project not found"));
        } catch (RuntimeException exception) {
            workspaceReady = false;
            addReadinessMessage(messages, "PROJECT_MISSING", "Project hierarchy is not ready.");
        }

        CreativeTool tool = toolRepository.findByToolCodeAndDeletedFalse(TOOL_CODE).orElse(null);
        WorkspacePlanContextView planContext = null;
        try {
            planContext = planContextService.getWorkspacePlanContext(workspaceId);
            if (planContext.pricingPlan() == null && planContext.subscription() == null) {
                packageReady = false;
                addReadinessMessage(messages, "PACKAGE_MISSING", "Workspace has no active package.");
            }
            if (!isPolicyReadyForReadiness(planContext.featurePolicy(), messages)) {
                packageReady = false;
            }
        } catch (RuntimeException exception) {
            packageReady = false;
            addReadinessMessage(messages, "PACKAGE_MISSING", "Workspace has no active package.");
        }

        ImageCreativeQualityMode qualityMode = parseQualityMode(requestedQualityMode);
        BigDecimal totalCreditCost = BigDecimal.ZERO;
        if (tool != null) {
            try {
                totalCreditCost = resolveCreditCost(tool.getId(), qualityMode).multiply(BigDecimal.valueOf(Math.max(1, requestedVersionCount)));
                CreditBalanceView balance = creditBalanceService.getBalance(workspaceId);
                if (balance.availableBalance() == null || balance.availableBalance().compareTo(totalCreditCost) < 0) {
                    creditsReady = false;
                    addReadinessMessage(messages, "CREDITS_INSUFFICIENT", "Workspace has insufficient credits.");
                }
            } catch (RuntimeException exception) {
                creditsReady = false;
                addReadinessMessage(messages, "CREDIT_POLICY_MISSING", "Master tool credit cost policy is not configured.");
            }
        } else {
            routingReady = false;
            providerReady = false;
            addReadinessMessage(messages, "TOOL_MISSING", "CAMPAIGN_CREATIVE_GENERATOR creative tool is not configured by Master.");
        }

        if (tool != null) {
            try {
                ResolvedProviderRouteView route = providerToolRegistryService.resolveProvider(tool.getId(), qualityMode.name());
                AiToolProvider routedProvider = route.providerId() == null
                        ? null
                        : providerRepository.findByIdAndDeletedFalse(route.providerId()).orElse(null);
                if (!isOpenAiProviderReady(routedProvider)) {
                    providerReady = false;
                    addReadinessMessage(messages, "PROVIDER_NOT_CONFIGURED", "No active OpenAI or image provider is configured by Master.");
                } else if (!hasActiveConfiguredCredential(routedProvider.getId())) {
                    providerReady = false;
                    addReadinessMessage(messages, "PROVIDER_CREDENTIAL_MISSING", "OpenAI credential is missing or inactive.");
                }
            } catch (RuntimeException exception) {
                routingReady = false;
                providerReady = false;
                addReadinessMessage(messages, "PROVIDER_ROUTING_MISSING", "Configure OpenAI in Master Provider Settings and Provider Routing.");
            }
        }

        if (productAssetId == null) {
            productAssetReady = false;
            addReadinessMessage(messages, "PRODUCT_IMAGE_MISSING", "Upload or select a READY product image before generating creative.");
        } else {
            try {
                resolveProductAsset(workspaceId, projectId, productAssetId, true);
            } catch (RuntimeException exception) {
                productAssetReady = false;
                addReadinessMessage(messages, "PRODUCT_IMAGE_NOT_READY", productAssetReadinessMessage(exception));
            }
        }

        boolean ready = workspaceReady && packageReady && creditsReady && providerReady && routingReady && productAssetReady;
        return new ProductImageCreativeReadinessView(
                ready,
                workspaceReady,
                packageReady,
                creditsReady,
                providerReady,
                routingReady,
                productAssetReady,
                messages.stream().map(ProductImageCreativeReadinessView.ReadinessMessage::message).distinct().toList(),
                messages.stream().distinct().toList());
    }

    @Transactional
    public ProductImageCreativeGenerationResult generate(ProductImageCreativeCommand command) {
        ValidationContext context = validate(command);
        ImageCreativeGeneration generation = generationRepository.save(ImageCreativeGeneration.requested(
                context.workspaceId(),
                context.project().getId(),
                context.brand().getId(),
                context.product() == null ? null : context.product().getId(),
                context.productAsset() == null ? null : context.productAsset().getId(),
                context.tool().getToolCode(),
                context.request().creativeFormat(),
                context.request().platform(),
                context.request().language(),
                context.qualityMode(),
                context.requestedVersionCount(),
                context.totalCreditCost(),
                requestPayload(context)));
        publish(KafkaTopicConstants.IMAGE_CREATIVE_GENERATION_REQUESTED, context.workspaceId(), generation.getId(), Map.of(
                "toolCode", context.tool().getToolCode(),
                "requestedVersionCount", context.requestedVersionCount()));

        CreditReservationResult reservation = null;
        try {
            if (context.totalCreditCost().signum() > 0) {
                reservation = creditReservationService.reserveCredits(context.workspaceId(), context.totalCreditCost(), REFERENCE_TYPE, generation.getId());
                generation.attachCreditReservation(reservation.reservationId());
                publish(KafkaTopicConstants.CREDITS_RESERVED, context.workspaceId(), generation.getId(), Map.of("amount", context.totalCreditCost()));
            }

            CreativeRequestEntity creativeRequest = createCreativeRequest(context, generation.getCreditReservationId());
            creativeRequest = creativeRequestRepository.save(creativeRequest);
            generation.attachCreativeRequest(creativeRequest.getId());
            generation = generationRepository.save(generation);

            ResolvedProviderRouteView route = requireResolvedOpenAiRoute(context.tool().getId(), context.qualityMode().name());
            ProductImageCreativeProvider generationProvider = provider.getIfAvailable(() ->
                    new ProductImageCreativeProvider() {
                        @Override
                        public List<ProductImageCreativeProviderOutput> generate(ProductImageCreativeContext context, int count, ResolvedProviderRouteView route) {
                            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_UNAVAILABLE, "OpenAI product image creative provider is not registered");
                        }
                    });
            List<ProductImageCreativeProviderOutput> outputs = generationProvider.generate(new ProductImageCreativeContext(
                    generation.getId(),
                    context.tool().getToolCode(),
                    context.project(),
                    context.brand(),
                    context.product(),
                    context.productAsset(),
                    context.request()), context.requestedVersionCount(), route);

            List<GeneratedVersionEntity> versions = new ArrayList<>();
            int versionNumber = 1;
            for (ProductImageCreativeProviderOutput output : outputs) {
                AssetEntity generatedAsset = assetRepository.saveAndFlush(createGeneratedAsset(context, output, versionNumber));
                StorageService.StoredObject storedObject = storeGeneratedAsset(context, generatedAsset.getId(), output);
                generatedAsset.completeUpload(
                        storedObject.storedFileName(),
                        AssetFileType.IMAGE,
                        output.mimeType(),
                        output.fileExtension(),
                        output.content().length,
                        storageService.provider(),
                        storedObject.bucket(),
                        storedObject.storageKey(),
                        storedObject.publicUrl(),
                        storedObject.previewUrl(),
                        storedObject.thumbnailUrl(),
                        output.width(),
                        output.height(),
                        null);
                StorageFileEntity storageFile = storageFileService.registerGeneratedOutput(
                        context.workspaceId(),
                        context.project().getId(),
                        storageService.provider(),
                        storedObject.bucket(),
                        storedObject.storageKey(),
                        storedObject.publicUrl(),
                        output.mimeType(),
                        output.fileExtension(),
                        output.content().length,
                        output.width(),
                        output.height(),
                        null,
                        output.content());
                generatedAsset.attachStorageFile(storageFile.getId());
                generatedAsset = assetRepository.save(generatedAsset);
                publish(KafkaTopicConstants.ASSET_GENERATED_METADATA_CREATED, context.workspaceId(), generatedAsset.getId(), Map.of(
                        "assetId", generatedAsset.getId().toString(),
                        "storageProvider", generatedAsset.getStorageProvider().name()));
                GeneratedVersionEntity version = GeneratedVersionEntity.create(
                        context.workspaceId(),
                        creativeRequest.getId(),
                        context.project().getId(),
                        versionNumber,
                        "Image Creative v%s".formatted(versionNumber),
                        generatedAsset.getStorageFileId(),
                        generatedAsset.getId(),
                        GenerationStatus.READY,
                        ApprovalStatus.NOT_SUBMITTED,
                        true,
                        route.providerId().toString(),
                        route.modelId() == null ? null : route.modelId().toString(),
                        currentUserId(),
                        GeneratedVersionStatus.ACTIVE);
                version.recordGeneratedAsset(generatedAsset.getId(), generatedAsset.getId(), generatedAsset.getId(), 0L, context.unitCreditCost(), BigDecimal.ZERO, output.width(), output.height(), output.fileExtension());
                versions.add(generatedVersionRepository.save(version));
                publish(KafkaTopicConstants.GENERATED_VERSION_CREATED, context.workspaceId(), version.getId(), Map.of("generatedVersionId", version.getId().toString()));
                scoreQuality(context, version, route);
                versionNumber++;
            }

            creativeRequest.markGenerationCompleted(Instant.now(), versions.size());
            creativeRequestRepository.save(creativeRequest);
            List<UUID> versionIds = versions.stream().map(GeneratedVersionEntity::getId).toList();
            generation.complete(versionIds);
            generation = generationRepository.save(generation);

            if (reservation != null) {
                creditReservationService.finalizeCredits(new CreditFinalizeCommand(context.workspaceId(), reservation.reservationId(), REFERENCE_TYPE, generation.getId(), "image_creative_generation_completed"));
                publish(KafkaTopicConstants.CREDITS_FINALIZED, context.workspaceId(), generation.getId(), Map.of("amount", context.totalCreditCost()));
            }
            UsageBillingLog usageLog = usageBillingLogRepository.save(UsageBillingLog.create(
                    context.workspaceId(),
                    "IMAGE_CREATIVE_GENERATION",
                    REFERENCE_TYPE,
                    generation.getId(),
                    context.totalCreditCost(),
                    null,
                    context.planContext().pricingPlan() == null ? null : context.planContext().pricingPlan().id(),
                    context.planContext().featurePolicy() == null ? null : context.planContext().featurePolicy().id()));
            publish(KafkaTopicConstants.USAGE_EVENT_RECORDED, context.workspaceId(), usageLog.getId(), Map.of("referenceId", generation.getId().toString()));
            publish(KafkaTopicConstants.IMAGE_CREATIVE_GENERATION_COMPLETED, context.workspaceId(), generation.getId(), Map.of("generatedVersionCount", versions.size()));

            List<GeneratedVersionView> versionViews = versions.stream().map(generatedVersionViewMapper::toView).toList();
            return new ProductImageCreativeGenerationResult(mapper.toView(generation), versionViews);
        } catch (RuntimeException ex) {
            generation.fail(ex.getMessage());
            generationRepository.save(generation);
            if (reservation != null) {
                creditReservationService.refundCredits(new CreditRefundCommand(context.workspaceId(), reservation.reservationId(), REFERENCE_TYPE, generation.getId(), "image_creative_generation_failed"));
                publish(KafkaTopicConstants.CREDITS_REFUNDED, context.workspaceId(), generation.getId(), Map.of("amount", context.totalCreditCost()));
            }
            publish(KafkaTopicConstants.IMAGE_CREATIVE_GENERATION_FAILED, context.workspaceId(), generation.getId(), Map.of(
                    "reason", ex.getMessage() == null ? "generation_failed" : ex.getMessage()));
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public PagedResult<ImageCreativeGenerationView> history(UUID workspaceId, UUID projectId, Pageable pageable) {
        projectCampaignRepository.findByIdAndWorkspaceIdAndDeletedFalse(projectId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Campaign not found"));
        return PagedResult.from(generationRepository
                .findAllByWorkspaceIdAndProjectIdAndDeletedFalseOrderByCreatedAtDesc(workspaceId, projectId, pageable)
                .map(mapper::toView));
    }

    private ValidationContext validate(ProductImageCreativeCommand command) {
        UUID workspaceId = require(command.workspaceId(), "workspaceId");
        UUID projectId = require(command.projectId(), "projectId");
        ProductImageCreativeRequest request = require(command.request(), "request");
        if (request.sourcePrompt() == null || request.sourcePrompt().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "sourcePrompt is required");
        }
        ImageCreativeFormat format = require(request.creativeFormat(), "creativeFormat");
        if (!format.supports(require(request.platform(), "platform"))) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "creativeFormat is not valid for platform");
        }

        ProjectCampaignEntity project = projectCampaignRepository.findByIdAndWorkspaceIdAndDeletedFalse(projectId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Campaign not found"));
        BrandEntity brand = brandRepository.findByIdAndWorkspaceIdAndDeletedFalse(project.getBrandId(), workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Brand not found"));
        validateLanguage(brand, request.language());

        CreativeTool tool = toolRepository.findByToolCodeAndDeletedFalse(TOOL_CODE)
                .filter(CreativeTool::isEnabled)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Product image creative tool is not configured"));
        WorkspacePlanContextView planContext = planContextService.getWorkspacePlanContext(workspaceId);
        ImageCreativeQualityMode qualityMode = request.qualityMode() == null ? ImageCreativeQualityMode.BASIC : request.qualityMode();
        int count = request.requestedVersionCount() == null ? 1 : request.requestedVersionCount();
        requirePackageAllows(planContext.featurePolicy(), tool.getToolCode(), qualityMode, count);
        boolean productAssetRequired = isProductAssetRequired(tool);
        AssetEntity productAsset = resolveProductAsset(workspaceId, projectId, request.productAssetId(), productAssetRequired);
        ProductServiceEntity product = resolveProduct(workspaceId, brand.getId(), project.getProductServiceId(), productAsset);
        if (product == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Product/service context is required for image creative generation");
        }
        BigDecimal unitCost = resolveCreditCost(tool.getId(), qualityMode);
        BigDecimal totalCost = unitCost.multiply(BigDecimal.valueOf(count));
        return new ValidationContext(workspaceId, project, brand, product, productAsset, tool, planContext, request, qualityMode, count, unitCost, totalCost);
    }

    private void requirePackageAllows(PlanFeaturePolicyView policy, String toolCode, ImageCreativeQualityMode qualityMode, int count) {
        if (policy == null) {
            throw new BusinessException(ErrorCode.PLAN_FEATURE_DISABLED, "CAMPAIGN_CREATIVE_GENERATOR is not enabled in plan feature policy.");
        }
        Set<String> enabledCodes = policy.enabledCreativeToolCodes();
        if (!policy.creativeGenerationEnabled() && (enabledCodes == null || !enabledCodes.contains(toolCode))) {
            throw new BusinessException(ErrorCode.PLAN_FEATURE_DISABLED, "CAMPAIGN_CREATIVE_GENERATOR is not enabled in plan feature policy.");
        }
        if (enabledCodes == null || !enabledCodes.contains(toolCode)) {
            throw new BusinessException(ErrorCode.PLAN_FEATURE_DISABLED, "CAMPAIGN_CREATIVE_GENERATOR is not enabled in plan feature policy.");
        }
        if (qualityMode == ImageCreativeQualityMode.PREMIUM && !policy.premiumQualityEnabled()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Package does not allow premium image creative generation");
        }
        if (count < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "requestedVersionCount must be greater than zero");
        }
        Integer limit = policy.maxGeneratedVersionsPerRequest();
        if (limit != null && count > limit) {
            throw new BusinessException(ErrorCode.PLAN_QUOTA_EXCEEDED, "requestedVersionCount exceeds plan policy");
        }
    }

    private AssetEntity resolveProductAsset(UUID workspaceId, UUID projectId, UUID productAssetId, boolean required) {
        if (productAssetId == null) {
            if (required) {
                throw productAssetValidation("ASSET_REQUIRED", "Upload or select a READY product image before generating creative.");
            }
            return null;
        }
        AssetEntity asset = assetRepository.findByIdAndWorkspaceIdAndDeletedFalse(productAssetId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASSET_NOT_FOUND));
        if (asset.getProjectId() != null && !projectId.equals(asset.getProjectId())) {
            throw productAssetValidation("ASSET_PROJECT_MISMATCH", "Selected product image does not belong to this project.");
        }
        boolean productImage = asset.getAssetCategory() == AssetCategory.PRODUCT_IMAGE
                || asset.getAssetCategory() == AssetCategory.REFERENCE_IMAGE
                || asset.getAssetType() == AssetType.PRODUCT_IMAGE
                || asset.getAssetType() == AssetType.RAW_IMAGE;
        if (!productImage) {
            throw productAssetValidation("ASSET_TYPE_INVALID", "Select a product or reference image before generating creative.");
        }
        if (!isSupportedProductImageAsset(asset)) {
            throw productAssetValidation("ASSET_FILE_TYPE_INVALID", "Selected product image must be JPG, PNG, or WebP.");
        }
        if (!asset.isReady()) {
            throw productAssetValidation("ASSET_NOT_READY", "Upload or select a READY product image before generating creative.");
        }
        return asset;
    }

    private BusinessException productAssetValidation(String code, String message) {
        return new BusinessException(
                ErrorCode.VALIDATION_FAILED,
                "Selected product image is not ready for generation",
                List.of(ApiError.of(code, "productAssetId", message)));
    }

    private String productAssetReadinessMessage(RuntimeException exception) {
        if (exception instanceof BusinessException businessException) {
            return businessException.getErrors().stream()
                    .filter(error -> "productAssetId".equals(error.field()))
                    .map(ApiError::message)
                    .filter(message -> message != null && !message.isBlank())
                    .findFirst()
                    .orElse(businessException.getMessage());
        }
        return "Product image readiness could not be verified: " + exception.getClass().getSimpleName();
    }

    private boolean isSupportedProductImageMimeType(String mimeType) {
        return "image/jpeg".equalsIgnoreCase(mimeType)
                || "image/png".equalsIgnoreCase(mimeType)
                || "image/webp".equalsIgnoreCase(mimeType);
    }

    private boolean isSupportedProductImageAsset(AssetEntity asset) {
        boolean supportedMimeType = isSupportedProductImageMimeType(asset.getMimeType());
        boolean imageFileType = asset.getFileType() == AssetFileType.IMAGE
                || (asset.getFileType() == null && supportedMimeType);
        return imageFileType && supportedMimeType;
    }

    private ProductServiceEntity resolveProduct(UUID workspaceId, UUID brandId, UUID campaignProductServiceId, AssetEntity productAsset) {
        if (productAsset != null && productAsset.getProductServiceId() != null) {
            ProductServiceEntity product = productRepository.findByIdAndWorkspaceIdAndDeletedFalse(productAsset.getProductServiceId(), workspaceId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Product/service not found"));
            if (!brandId.equals(product.getBrandId())) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Product/service does not belong to the brand");
            }
            return product;
        }
        if (campaignProductServiceId != null) {
            ProductServiceEntity product = productRepository.findByIdAndWorkspaceIdAndDeletedFalse(campaignProductServiceId, workspaceId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Campaign product/service not found"));
            if (!brandId.equals(product.getBrandId())) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Campaign product/service does not belong to the brand");
            }
            return product;
        }
        List<ProductServiceEntity> products = productRepository.findAllByWorkspaceIdAndBrandIdAndDeletedFalseOrderByCreatedAtDesc(workspaceId, brandId);
        return products.size() == 1 ? products.getFirst() : null;
    }

    private void validateLanguage(BrandEntity brand, PromptLanguage language) {
        PromptLanguage requested = require(language, "language");
        BrandLanguagePreference preference = brand.getLanguagePreference();
        boolean allowed = preference == BrandLanguagePreference.BOTH
                || (preference == BrandLanguagePreference.BANGLA && requested == PromptLanguage.BANGLA)
                || (preference == BrandLanguagePreference.ENGLISH && requested == PromptLanguage.ENGLISH);
        if (!allowed) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Language does not match brand language preference");
        }
    }

    private boolean isProductAssetRequired(CreativeTool tool) {
        Object value = tool.getMetadata().get("productAssetRequired");
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    private BigDecimal resolveCreditCost(UUID toolId, ImageCreativeQualityMode qualityMode) {
        Instant now = Instant.now();
        return costPolicyRepository.findAllByToolIdAndDeletedFalseOrderByPolicyCodeAsc(toolId).stream()
                .filter(ToolCreditCostPolicy::isEnabled)
                .filter(policy -> matchesQualityMode(policy, qualityMode))
                .filter(policy -> isEffective(policy, now))
                .map(ToolCreditCostPolicy::getCreditCost)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Master tool credit cost policy is not configured"));
    }

    private boolean matchesQualityMode(ToolCreditCostPolicy policy, ImageCreativeQualityMode qualityMode) {
        Object metadataQualityMode = policy.getMetadata().get("qualityMode");
        if (metadataQualityMode == null) {
            return qualityMode == ImageCreativeQualityMode.BASIC;
        }
        return qualityMode.name().equalsIgnoreCase(String.valueOf(metadataQualityMode));
    }

    private boolean isEffective(ToolCreditCostPolicy policy, Instant now) {
        return (policy.getEffectiveFrom() == null || !policy.getEffectiveFrom().isAfter(now))
                && (policy.getEffectiveUntil() == null || policy.getEffectiveUntil().isAfter(now));
    }

    private CreativeRequestEntity createCreativeRequest(ValidationContext context, UUID reservationId) {
        CreativeRequestEntity request = CreativeRequestEntity.create(
                context.workspaceId(),
                context.brand().getId(),
                context.product().getId(),
                context.project().getId(),
                currentUserId(),
                "Product Image Creative",
                context.request().sourcePrompt(),
                null,
                brandLanguage(context.request().language()),
                context.request().platform(),
                CreativeType.STATIC_IMAGE,
                parseCampaignObjective(context.project().getCampaignObjective()),
                context.request().stylePreset(),
                context.brand().getTargetAudience(),
                context.request().cta(),
                CreativeRequestStatus.PROCESSING,
                context.requestedVersionCount());
        request.replaceSelectedAssetIds(context.productAsset() == null ? List.of() : List.of(context.productAsset().getId()));
        request.attachCreditReservation(reservationId);
        return request;
    }

    private AssetEntity createGeneratedAsset(ValidationContext context, ProductImageCreativeProviderOutput output, int versionNumber) {
        Map<String, Object> metadata = new LinkedHashMap<>(output.metadata());
        metadata.put("r2ObjectKey", output.objectKey());
        metadata.put("creativeFormat", context.request().creativeFormat().name());
        metadata.put("sourceProductAssetId", context.productAsset() == null ? null : context.productAsset().getId().toString());
        AssetEntity asset = AssetEntity.createUploading(
                context.workspaceId(),
                context.brand().getId(),
                context.product() == null ? null : context.product().getId(),
                context.project().getId(),
                currentUserId(),
                null,
                AssetType.GENERATED_CREATIVE,
                AssetCategory.GENERATED_CREATIVE,
                output.fileName(),
                "Generated Image Creative %s".formatted(versionNumber),
                context.request().sourcePrompt(),
                Set.of("generated", "image-creative"),
                null,
                metadataJson(metadata),
                storageService.provider());
        return asset;
    }

    private StorageService.StoredObject storeGeneratedAsset(
            ValidationContext context,
            UUID assetId,
            ProductImageCreativeProviderOutput output
    ) {
        if (output.content() == null || output.content().length == 0) {
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_RESPONSE_INVALID, "Provider returned an empty image creative artifact");
        }
        return storageService.storeGenerated(new StorageService.GeneratedStorageUploadRequest(
                context.workspaceId(),
                context.project().getId(),
                assetId,
                output.fileExtension(),
                output.mimeType(),
                output.content()));
    }

    private void scoreQuality(ValidationContext context, GeneratedVersionEntity version, ResolvedProviderRouteView route) {
        qualityService.scoreGeneratedVersion(new QualityScoreInput(
                context.workspaceId(),
                version.getId(),
                route.providerId(),
                null,
                route.modelId() == null ? null : route.modelId().toString(),
                BigDecimal.valueOf(80),
                BigDecimal.valueOf(context.productAsset() == null ? 70 : 90),
                BigDecimal.valueOf(85),
                context.request().language() == PromptLanguage.BANGLA ? BigDecimal.valueOf(80) : BigDecimal.ZERO,
                BigDecimal.valueOf(82),
                BigDecimal.valueOf(84),
                "Initial deterministic product image creative quality score.",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                true));
    }

    private BrandLanguagePreference brandLanguage(PromptLanguage language) {
        if (language == PromptLanguage.BANGLA) {
            return BrandLanguagePreference.BANGLA;
        }
        if (language == PromptLanguage.ENGLISH) {
            return BrandLanguagePreference.ENGLISH;
        }
        return BrandLanguagePreference.BOTH;
    }

    private CampaignObjective parseCampaignObjective(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return CampaignObjective.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Map<String, Object> requestPayload(ValidationContext context) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sourcePrompt", context.request().sourcePrompt());
        payload.put("creativeFormat", context.request().creativeFormat().name());
        payload.put("platform", context.request().platform().name());
        payload.put("language", context.request().language().name());
        payload.put("qualityMode", context.qualityMode().name());
        putIfPresent(payload, "promptDraftId", context.request().promptDraftId() == null ? null : context.request().promptDraftId().toString());
        putIfPresent(payload, "stylePreset", context.request().stylePreset());
        putIfPresent(payload, "backgroundStyle", context.request().backgroundStyle());
        putIfPresent(payload, "cta", context.request().cta());
        return payload;
    }

    private void putIfPresent(Map<String, Object> payload, String key, Object value) {
        if (value != null) {
            payload.put(key, value);
        }
    }

    private String metadataJson(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Generated asset metadata serialization failed");
        }
    }

    private UUID currentUserId() {
        if (currentUserContext == null) {
            return SYSTEM_USER_ID;
        }
        return currentUserContext.getCurrentUser().map(user -> user.userId()).orElse(SYSTEM_USER_ID);
    }

    private void publish(String topic, UUID workspaceId, UUID aggregateId, Map<String, Object> attributes) {
        if (domainEventPublisher != null) {
            domainEventPublisher.publish(topic, new BaseDomainEvent(topic, workspaceId, aggregateId, Instant.now(), attributes));
        }
    }

    private <T> T require(T value, String field) {
        if (value == null) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    field + " is required",
                    List.of(ApiError.of("FIELD_REQUIRED", field, field + " is required")));
        }
        return value;
    }

    private UUID requireBusiness(UUID value, String field) {
        return require(value, field);
    }

    private boolean isPolicyReadyForReadiness(
            PlanFeaturePolicyView policy,
            List<ProductImageCreativeReadinessView.ReadinessMessage> messages
    ) {
        if (policy == null || !policy.creativeGenerationEnabled()) {
            addReadinessMessage(messages, "PACKAGE_FEATURE_DISABLED", "CAMPAIGN_CREATIVE_GENERATOR is not enabled in plan feature policy.");
            return false;
        }
        Set<String> enabledCodes = policy.enabledCreativeToolCodes();
        if (enabledCodes == null || !enabledCodes.contains(TOOL_CODE)) {
            addReadinessMessage(messages, "PACKAGE_TOOL_DISABLED", "CAMPAIGN_CREATIVE_GENERATOR is not enabled in plan feature policy.");
            return false;
        }
        if (policy.maxGeneratedVersionsPerRequest() == null || policy.maxGeneratedVersionsPerRequest() < 1) {
            addReadinessMessage(messages, "PACKAGE_VERSION_LIMIT_MISSING", "Package generated-version limit is not configured.");
            return false;
        }
        return true;
    }

    private void addReadinessMessage(
            List<ProductImageCreativeReadinessView.ReadinessMessage> messages,
            String code,
            String message
    ) {
        messages.add(new ProductImageCreativeReadinessView.ReadinessMessage(code, message));
    }

    private ResolvedProviderRouteView requireResolvedOpenAiRoute(UUID toolId, String qualityMode) {
        try {
            ResolvedProviderRouteView route = providerToolRegistryService.resolveProvider(toolId, qualityMode);
            AiToolProvider routedProvider = route.providerId() == null
                    ? null
                    : providerRepository.findByIdAndDeletedFalse(route.providerId()).orElse(null);
            if (!isOpenAiProviderReady(routedProvider) || !hasActiveConfiguredCredential(routedProvider.getId())) {
                throw providerUnavailable();
            }
            return route;
        } catch (BusinessException exception) {
            if (exception.getErrorCode() == ErrorCode.GENERATION_PROVIDER_UNAVAILABLE
                    || exception.getErrorCode() == ErrorCode.RESOURCE_NOT_FOUND
                    || exception.getErrorCode() == ErrorCode.AI_ROUTING_POLICY_INVALID) {
                throw providerUnavailable();
            }
            throw exception;
        }
    }

    private BusinessException providerUnavailable() {
        return new BusinessException(
                ErrorCode.AI_ROUTING_POLICY_INVALID,
                "No active AI provider is configured for image creative generation.",
                List.of(ApiError.of(
                        "PROVIDER_ROUTING_MISSING",
                        "provider",
                        "Configure OpenAI in Master Provider Settings and Provider Routing.")));
    }

    private boolean isOpenAiProviderReady(AiToolProvider provider) {
        return provider != null
                && "OPENAI".equalsIgnoreCase(provider.getProviderCode())
                && supportsImageGeneration(provider)
                && provider.isEnabled()
                && provider.getStatus() == ProviderStatus.ACTIVE;
    }

    private boolean supportsImageGeneration(AiToolProvider provider) {
        List<String> layers = provider.getSupportedLayers();
        if (layers == null || layers.isEmpty()) {
            return true;
        }
        return layers.stream().anyMatch(layer ->
                "IMAGE".equalsIgnoreCase(layer)
                        || "STATIC_IMAGE".equalsIgnoreCase(layer)
                        || "GENERATED_CREATIVE".equalsIgnoreCase(layer));
    }

    private boolean hasActiveConfiguredCredential(UUID providerId) {
        if (providerId == null) {
            return false;
        }
        return credentialRepository.findFirstByProviderIdAndActiveTrueAndDeletedFalseOrderByUpdatedAtDesc(providerId)
                .filter(AiProviderCredential::isActive)
                .filter(credential -> credential.getCredentialStatus() == CredentialStatus.CONFIGURED)
                .filter(credential -> credential.getEncryptedSecret() != null && !credential.getEncryptedSecret().isBlank())
                .isPresent();
    }

    private ImageCreativeQualityMode parseQualityMode(String value) {
        try {
            return value == null || value.isBlank()
                    ? ImageCreativeQualityMode.BASIC
                    : ImageCreativeQualityMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return ImageCreativeQualityMode.BASIC;
        }
    }

    private record ValidationContext(
            UUID workspaceId,
            ProjectCampaignEntity project,
            BrandEntity brand,
            ProductServiceEntity product,
            AssetEntity productAsset,
            CreativeTool tool,
            WorkspacePlanContextView planContext,
            ProductImageCreativeRequest request,
            ImageCreativeQualityMode qualityMode,
            int requestedVersionCount,
            BigDecimal unitCreditCost,
            BigDecimal totalCreditCost
    ) {
    }
}
