package com.lebhas.creativesaas.generation.application;

import com.lebhas.ai.domain.AiModel;
import com.lebhas.ai.domain.AiToolProvider;
import com.lebhas.ai.domain.CredentialStatus;
import com.lebhas.ai.domain.CreativeLayerType;
import com.lebhas.ai.domain.CreativePipeline;
import com.lebhas.ai.domain.CreativePipelineLayer;
import com.lebhas.ai.domain.CreativePipelineStatus;
import com.lebhas.ai.domain.ProviderStatus;
import com.lebhas.ai.infrastructure.persistence.AiModelRepository;
import com.lebhas.ai.infrastructure.persistence.AiProviderCredentialRepository;
import com.lebhas.ai.infrastructure.persistence.AiToolProviderRepository;
import com.lebhas.ai.infrastructure.persistence.CreativePipelineLayerRepository;
import com.lebhas.ai.infrastructure.persistence.CreativePipelineRepository;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.generation.domain.CreativePipelineLayerRunEntity;
import com.lebhas.creativesaas.generation.domain.CreativePipelineRunEntity;
import com.lebhas.creativesaas.generation.infrastructure.persistence.CreativePipelineLayerRunRepository;
import com.lebhas.creativesaas.generation.infrastructure.persistence.CreativePipelineRunRepository;
import com.lebhas.creativesaas.imagecreative.application.dto.ProductImageCreativeRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class CreativePipelineOrchestrationService {

    private static final String INTERNAL_PROVIDER = "INTERNAL";
    private static final EnumSet<CreativeLayerType> SUPPORTED_LAYER_TYPES = EnumSet.of(
            CreativeLayerType.IMAGE_ANALYSIS,
            CreativeLayerType.BACKGROUND_REMOVAL,
            CreativeLayerType.IMAGE_CLEANUP,
            CreativeLayerType.PROMPT_GENERATION,
            CreativeLayerType.IMAGE_GENERATION,
            CreativeLayerType.TEXT_OVERLAY,
            CreativeLayerType.VISION_QUALITY_CHECK,
            CreativeLayerType.IMAGE_RESIZE,
            CreativeLayerType.IMAGE_EXPORT,
            CreativeLayerType.INTERNAL_SAVE);

    private final CreativePipelineRepository pipelineRepository;
    private final CreativePipelineLayerRepository pipelineLayerRepository;
    private final AiToolProviderRepository providerRepository;
    private final AiModelRepository modelRepository;
    private final AiProviderCredentialRepository credentialRepository;
    private final CreativePipelineRunRepository runRepository;
    private final CreativePipelineLayerRunRepository layerRunRepository;

    public CreativePipelineOrchestrationService(
            CreativePipelineRepository pipelineRepository,
            CreativePipelineLayerRepository pipelineLayerRepository,
            AiToolProviderRepository providerRepository,
            AiModelRepository modelRepository,
            AiProviderCredentialRepository credentialRepository,
            CreativePipelineRunRepository runRepository,
            CreativePipelineLayerRunRepository layerRunRepository
    ) {
        this.pipelineRepository = pipelineRepository;
        this.pipelineLayerRepository = pipelineLayerRepository;
        this.providerRepository = providerRepository;
        this.modelRepository = modelRepository;
        this.credentialRepository = credentialRepository;
        this.runRepository = runRepository;
        this.layerRunRepository = layerRunRepository;
    }

    @Transactional
    public CreativePipelineRunEntity planAndCreateRun(
            CreativeRequestEntity creativeRequest,
            ProductImageCreativeRequest request,
            String primaryProviderCode,
            String imageGenerationModelCode,
            BigDecimal estimatedCreditCost,
            UUID productAssetId
    ) {
        String normalizedPrimaryProvider = normalizeProviderCode(primaryProviderCode);
        AiToolProvider primaryProvider = requireProviderReady(normalizedPrimaryProvider, CreativeLayerType.IMAGE_GENERATION);
        if (imageGenerationModelCode != null && !imageGenerationModelCode.isBlank()) {
            requireModelAllowed(primaryProvider, imageGenerationModelCode);
        }

        List<PlannedLayer> layers = defaultPlan(
                normalizedPrimaryProvider,
                imageGenerationModelCode,
                estimatedCreditCost,
                productAssetId,
                activeConfiguredLayerTypes());
        validateRequiredLayers(layers);

        Map<String, Object> planJson = planJson(creativeRequest, request, normalizedPrimaryProvider, layers, estimatedCreditCost);
        CreativePipelineRunEntity run = runRepository.save(CreativePipelineRunEntity.planned(
                creativeRequest.getWorkspaceId(),
                creativeRequest.getId(),
                normalizedPrimaryProvider,
                strategy(layers, normalizedPrimaryProvider),
                planJson,
                normalizeCost(estimatedCreditCost)));
        for (PlannedLayer layer : layers) {
            layerRunRepository.save(CreativePipelineLayerRunEntity.planned(
                    run.getId(),
                    creativeRequest.getId(),
                    layer.sequence(),
                    layer.layerType(),
                    layer.providerCode(),
                    layer.modelCode(),
                    layer.inputAssetIds(),
                    layer.estimatedCost()));
        }
        return run;
    }

    @Transactional
    public void markRunProcessing(UUID pipelineRunId) {
        runRepository.findById(pipelineRunId).ifPresent(run -> {
            run.markProcessing();
            runRepository.save(run);
        });
    }

    @Transactional
    public void completeLayer(UUID pipelineRunId, CreativeLayerType layerType, Map<String, Object> output, List<UUID> outputAssetIds, BigDecimal actualCost) {
        layerRunRepository.findAllByPipelineRunIdOrderBySequenceAsc(pipelineRunId).stream()
                .filter(layer -> layer.getLayerType() == layerType)
                .findFirst()
                .ifPresent(layer -> {
                    layer.markStarted(Map.of("layerType", layerType.name()));
                    layer.markCompleted(output, outputAssetIds, normalizeCost(actualCost));
                    layerRunRepository.save(layer);
                });
    }

    @Transactional
    public void skipLayer(UUID pipelineRunId, CreativeLayerType layerType, String reason) {
        layerRunRepository.findAllByPipelineRunIdOrderBySequenceAsc(pipelineRunId).stream()
                .filter(layer -> layer.getLayerType() == layerType)
                .findFirst()
                .ifPresent(layer -> {
                    layer.markSkipped(reason);
                    layerRunRepository.save(layer);
                });
    }

    @Transactional
    public void completeRun(UUID pipelineRunId, BigDecimal actualCreditCost) {
        runRepository.findById(pipelineRunId).ifPresent(run -> {
            run.markCompleted(normalizeCost(actualCreditCost));
            runRepository.save(run);
        });
    }

    @Transactional
    public void failRun(UUID pipelineRunId, String reason) {
        runRepository.findById(pipelineRunId).ifPresent(run -> {
            run.markFailed(reason);
            runRepository.save(run);
        });
    }

    private List<CreativeLayerType> activeConfiguredLayerTypes() {
        Optional<CreativePipeline> active = pipelineRepository.findFirstByActiveTrueAndStatusAndDeletedFalse(CreativePipelineStatus.ACTIVE);
        if (active.isEmpty()) {
            return List.of(
                    CreativeLayerType.IMAGE_ANALYSIS,
                    CreativeLayerType.PROMPT_GENERATION,
                    CreativeLayerType.IMAGE_GENERATION,
                    CreativeLayerType.VISION_QUALITY_CHECK,
                    CreativeLayerType.IMAGE_EXPORT,
                    CreativeLayerType.INTERNAL_SAVE);
        }
        List<CreativeLayerType> configured = pipelineLayerRepository
                .findAllByPipelineIdAndDeletedFalseOrderBySortOrderAsc(active.get().getId()).stream()
                .filter(CreativePipelineLayer::isEnabled)
                .map(CreativePipelineLayer::getLayerType)
                .filter(SUPPORTED_LAYER_TYPES::contains)
                .distinct()
                .toList();
        return configured.isEmpty() ? List.of(CreativeLayerType.IMAGE_GENERATION, CreativeLayerType.IMAGE_EXPORT, CreativeLayerType.INTERNAL_SAVE) : configured;
    }

    private List<PlannedLayer> defaultPlan(
            String primaryProviderCode,
            String imageGenerationModelCode,
            BigDecimal totalCreditCost,
            UUID productAssetId,
            List<CreativeLayerType> configuredLayerTypes
    ) {
        List<CreativeLayerType> ordered = new ArrayList<>(configuredLayerTypes);
        ensure(ordered, CreativeLayerType.PROMPT_GENERATION);
        ensure(ordered, CreativeLayerType.IMAGE_GENERATION);
        ensure(ordered, CreativeLayerType.IMAGE_EXPORT);
        ensure(ordered, CreativeLayerType.INTERNAL_SAVE);
        ordered.sort(Comparator.comparingInt(this::defaultSortOrder));

        List<PlannedLayer> layers = new ArrayList<>();
        int sequence = 1;
        for (CreativeLayerType layerType : ordered) {
            String providerCode = isInternalLayer(layerType) ? INTERNAL_PROVIDER : primaryProviderCode;
            String modelCode = isInternalLayer(layerType) ? internalModel(layerType) : imageGenerationModelCode;
            validateLayerProvider(layerType, providerCode, modelCode);
            layers.add(new PlannedLayer(
                    sequence++,
                    layerType,
                    providerCode,
                    modelCode,
                    requiredLayer(layerType),
                    inputRefs(layerType),
                    outputRef(layerType),
                    reason(layerType, providerCode),
                    estimatedLayerCost(layerType, totalCreditCost),
                    productAssetId == null ? List.of() : List.of(productAssetId)));
        }
        return layers;
    }

    private void validateLayerProvider(CreativeLayerType layerType, String providerCode, String modelCode) {
        if (!SUPPORTED_LAYER_TYPES.contains(layerType)) {
            throw new BusinessException(ErrorCode.AI_ROUTING_POLICY_INVALID, "Unsupported creative pipeline layer type: " + layerType);
        }
        if (INTERNAL_PROVIDER.equals(providerCode)) {
            return;
        }
        AiToolProvider provider = requireProviderReady(providerCode, layerType);
        if (modelCode != null && !modelCode.isBlank()) {
            requireModelAllowed(provider, modelCode);
        }
    }

    private AiToolProvider requireProviderReady(String providerCode, CreativeLayerType layerType) {
        AiToolProvider provider = providerRepository.findByProviderCodeAndDeletedFalse(normalizeProviderCode(providerCode))
                .orElseThrow(() -> new BusinessException(ErrorCode.GENERATION_PROVIDER_UNAVAILABLE, "Configured AI provider is not available"));
        if (!provider.isEnabled() || provider.getStatus() != ProviderStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_UNAVAILABLE, "Configured AI provider is disabled or unhealthy");
        }
        if (!supportsLayer(provider, layerType)) {
            throw new BusinessException(ErrorCode.AI_ROUTING_POLICY_INVALID, "Configured AI provider does not support layer " + layerType.name());
        }
        boolean credentialReady = credentialRepository.findFirstByProviderIdAndActiveTrueAndDeletedFalseOrderByUpdatedAtDesc(provider.getId())
                .filter(credential -> credential.getCredentialStatus() == CredentialStatus.CONFIGURED)
                .filter(credential -> credential.getEncryptedSecret() != null && !credential.getEncryptedSecret().isBlank())
                .isPresent();
        if (!credentialReady) {
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_UNAVAILABLE, "Configured AI provider credential is missing");
        }
        return provider;
    }

    private void requireModelAllowed(AiToolProvider provider, String modelCode) {
        AiModel model = modelRepository.findByProviderIdAndModelCodeAndDeletedFalse(provider.getId(), normalizeProviderCode(modelCode))
                .orElseThrow(() -> new BusinessException(ErrorCode.AI_ROUTING_POLICY_INVALID, "Configured AI model is not allowed by Master"));
        if (!model.isEnabled() || model.getStatus() != ProviderStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.AI_ROUTING_POLICY_INVALID, "Configured AI model is disabled by Master");
        }
    }

    private boolean supportsLayer(AiToolProvider provider, CreativeLayerType layerType) {
        List<String> supported = provider.getSupportedLayers();
        if (supported.isEmpty()) {
            return true;
        }
        return supported.stream().anyMatch(value ->
                value.equalsIgnoreCase(layerType.name())
                        || value.equalsIgnoreCase("IMAGE")
                        || value.equalsIgnoreCase("STATIC_IMAGE")
                        || value.equalsIgnoreCase("GENERATED_CREATIVE"));
    }

    private void validateRequiredLayers(List<PlannedLayer> layers) {
        if (layers.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_ROUTING_POLICY_INVALID, "Creative pipeline plan contains no layers");
        }
        if (layers.stream().noneMatch(layer -> layer.layerType() == CreativeLayerType.IMAGE_GENERATION)) {
            throw new BusinessException(ErrorCode.AI_ROUTING_POLICY_INVALID, "Creative pipeline plan is missing IMAGE_GENERATION");
        }
        if (layers.stream().noneMatch(layer -> layer.layerType() == CreativeLayerType.IMAGE_EXPORT)) {
            throw new BusinessException(ErrorCode.AI_ROUTING_POLICY_INVALID, "Creative pipeline plan is missing IMAGE_EXPORT");
        }
        if (layers.stream().noneMatch(layer -> layer.layerType() == CreativeLayerType.INTERNAL_SAVE)) {
            throw new BusinessException(ErrorCode.AI_ROUTING_POLICY_INVALID, "Creative pipeline plan is missing INTERNAL_SAVE");
        }
    }

    private Map<String, Object> planJson(
            CreativeRequestEntity creativeRequest,
            ProductImageCreativeRequest request,
            String primaryProviderCode,
            List<PlannedLayer> layers,
            BigDecimal estimatedCreditCost
    ) {
        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("pipelineId", "generated-by-backend");
        plan.put("strategy", strategy(layers, primaryProviderCode));
        plan.put("primaryProvider", primaryProviderCode);
        plan.put("goal", creativeRequest.getRequestName());
        plan.put("inputSummary", Map.of(
                "creativeType", request.creativeFormat().name(),
                "platform", request.platform().name(),
                "language", request.language().name(),
                "cta", request.cta() == null ? "" : request.cta(),
                "requestedVersions", creativeRequest.getRequestedVersions()));
        plan.put("layers", layers.stream().map(this::layerJson).toList());
        plan.put("estimatedTotalCost", normalizeCost(estimatedCreditCost));
        plan.put("estimatedLebhasCreditCost", normalizeCost(estimatedCreditCost));
        plan.put("fallbackPlan", Map.of("onLayerFailure", "RETRY_OR_FAIL_REQUIRED_LAYER", "maxRetriesPerLayer", 1));
        return plan;
    }

    private Map<String, Object> layerJson(PlannedLayer layer) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("sequence", layer.sequence());
        json.put("layerType", layer.layerType().name());
        json.put("providerCode", layer.providerCode());
        json.put("modelCode", layer.modelCode());
        json.put("required", layer.required());
        json.put("inputRefs", layer.inputRefs());
        json.put("outputRef", layer.outputRef());
        json.put("reason", layer.reason());
        json.put("estimatedCost", layer.estimatedCost());
        return json;
    }

    private BigDecimal estimatedLayerCost(CreativeLayerType layerType, BigDecimal totalCreditCost) {
        if (layerType == CreativeLayerType.IMAGE_GENERATION) {
            return normalizeCost(totalCreditCost);
        }
        return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeCost(BigDecimal cost) {
        return (cost == null ? BigDecimal.ZERO : cost).setScale(4, RoundingMode.HALF_UP);
    }

    private String normalizeProviderCode(String value) {
        return AiToolProvider.normalizeCode(value, "providerCode");
    }

    private boolean isInternalLayer(CreativeLayerType layerType) {
        return layerType == CreativeLayerType.IMAGE_RESIZE
                || layerType == CreativeLayerType.IMAGE_EXPORT
                || layerType == CreativeLayerType.INTERNAL_SAVE;
    }

    private String internalModel(CreativeLayerType layerType) {
        return layerType == CreativeLayerType.INTERNAL_SAVE ? "DATABASE" : "R2_EXPORT";
    }

    private boolean requiredLayer(CreativeLayerType layerType) {
        return layerType == CreativeLayerType.PROMPT_GENERATION
                || layerType == CreativeLayerType.IMAGE_GENERATION
                || layerType == CreativeLayerType.VISION_QUALITY_CHECK
                || layerType == CreativeLayerType.IMAGE_EXPORT
                || layerType == CreativeLayerType.INTERNAL_SAVE;
    }

    private void ensure(List<CreativeLayerType> layers, CreativeLayerType layerType) {
        if (!layers.contains(layerType)) {
            layers.add(layerType);
        }
    }

    private int defaultSortOrder(CreativeLayerType layerType) {
        return switch (layerType) {
            case IMAGE_ANALYSIS -> 10;
            case BACKGROUND_REMOVAL -> 20;
            case IMAGE_CLEANUP -> 30;
            case PROMPT_GENERATION -> 40;
            case IMAGE_GENERATION -> 50;
            case TEXT_OVERLAY -> 60;
            case VISION_QUALITY_CHECK -> 70;
            case IMAGE_RESIZE -> 80;
            case IMAGE_EXPORT -> 90;
            case INTERNAL_SAVE -> 100;
            default -> 999;
        };
    }

    private List<String> inputRefs(CreativeLayerType layerType) {
        return switch (layerType) {
            case IMAGE_ANALYSIS, BACKGROUND_REMOVAL, IMAGE_CLEANUP -> List.of("productImageAssetId");
            case PROMPT_GENERATION -> List.of("analysisJson", "adminRequestJson");
            case IMAGE_GENERATION -> List.of("finalProviderPrompt", "productImageAssetId");
            case VISION_QUALITY_CHECK, IMAGE_RESIZE, IMAGE_EXPORT -> List.of("generatedImageAssetId");
            case INTERNAL_SAVE -> List.of("finalExportAssetId", "qualityCheckJson");
            default -> List.of();
        };
    }

    private String outputRef(CreativeLayerType layerType) {
        return switch (layerType) {
            case IMAGE_ANALYSIS -> "analysisJson";
            case BACKGROUND_REMOVAL -> "cleanProductImageAssetId";
            case IMAGE_CLEANUP -> "cleanedImageAssetId";
            case PROMPT_GENERATION -> "finalProviderPrompt";
            case IMAGE_GENERATION -> "generatedImageAssetId";
            case TEXT_OVERLAY -> "textOverlayAssetId";
            case VISION_QUALITY_CHECK -> "qualityCheckJson";
            case IMAGE_RESIZE -> "resizedImageAssetId";
            case IMAGE_EXPORT -> "finalExportAssetId";
            case INTERNAL_SAVE -> "generatedVersionId";
            default -> "output";
        };
    }

    private String reason(CreativeLayerType layerType, String providerCode) {
        if (INTERNAL_PROVIDER.equals(providerCode)) {
            return "Internal Lebhas layer for secure R2 export, resize, or database save.";
        }
        return providerCode + " selected by validated Master routing for " + layerType.name() + ".";
    }

    private String strategy(List<PlannedLayer> layers, String primaryProviderCode) {
        boolean hybrid = layers.stream().anyMatch(layer ->
                !INTERNAL_PROVIDER.equals(layer.providerCode()) && !primaryProviderCode.equals(layer.providerCode()));
        return hybrid ? "HYBRID_PROVIDER_OPTIMIZED" : "PRIMARY_PROVIDER_FULL_EXECUTION";
    }

    private record PlannedLayer(
            int sequence,
            CreativeLayerType layerType,
            String providerCode,
            String modelCode,
            boolean required,
            List<String> inputRefs,
            String outputRef,
            String reason,
            BigDecimal estimatedCost,
            List<UUID> inputAssetIds
    ) {
    }
}
