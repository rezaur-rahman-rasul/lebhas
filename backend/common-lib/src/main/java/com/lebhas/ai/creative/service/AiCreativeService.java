package com.lebhas.ai.creative.service;

import com.lebhas.ai.creative.dto.AiCreativeGenerateRequest;
import com.lebhas.ai.creative.dto.AiCreativeResponse;
import com.lebhas.ai.creative.dto.OpenAiImageResponse;
import com.lebhas.ai.creative.dto.TextCreativeRequest;
import com.lebhas.ai.creative.enums.CreativePlatform;
import com.lebhas.ai.creative.enums.CreativeQuality;
import com.lebhas.ai.creative.enums.CreativeStatus;
import com.lebhas.ai.creative.enums.CreativeTone;
import com.lebhas.ai.creative.enums.CreativeType;
import com.lebhas.ai.creative.enums.GenerationMode;
import com.lebhas.ai.creative.enums.ModelQuality;
import com.lebhas.ai.creative.enums.OutputFormat;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.infrastructure.persistence.AssetRepository;
import com.lebhas.creativesaas.asset.storage.StorageService;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AiCreativeService {

    private final AiCreativePromptBuilderService promptBuilderService;
    private final OpenAiImageClient openAiImageClient;
    private final Base64ImageDecoderService decoderService;
    private final ImageValidationService imageValidationService;
    private final StorageService storageService;
    private final PromptTitleService promptTitleService;
    private final AiCreativePersistenceService persistenceService;
    private final AssetRepository assetRepository;

    public AiCreativeService(
            AiCreativePromptBuilderService promptBuilderService,
            OpenAiImageClient openAiImageClient,
            Base64ImageDecoderService decoderService,
            ImageValidationService imageValidationService,
            StorageService storageService,
            PromptTitleService promptTitleService,
            AiCreativePersistenceService persistenceService,
            AssetRepository assetRepository
    ) {
        this.promptBuilderService = promptBuilderService;
        this.openAiImageClient = openAiImageClient;
        this.decoderService = decoderService;
        this.imageValidationService = imageValidationService;
        this.storageService = storageService;
        this.promptTitleService = promptTitleService;
        this.persistenceService = persistenceService;
        this.assetRepository = assetRepository;
    }

    public AiCreativeResponse generate(
            AiCreativeGenerateRequest request,
            MultipartFile productImage,
            MultipartFile logoImage,
            MultipartFile referenceImage,
            MultipartFile maskImage,
            String backgroundPrompt
    ) {
        productImage = resolveExistingProductImage(request, productImage);
        validateFiles(productImage, logoImage, referenceImage, maskImage);
        OutputFormat outputFormat = request.outputFormat() == null ? OutputFormat.png : request.outputFormat();
        CreativeQuality quality = request.quality() == null ? CreativeQuality.fromModelQuality(request.modelQuality()) : request.quality();
        String size = resolveSize(request.creativeType(), request.size());
        String background = request.background() == null || request.background().isBlank() ? "opaque" : request.background().trim().toLowerCase();
        if ("transparent".equals(background)) {
            outputFormat = outputFormat == OutputFormat.jpeg ? OutputFormat.png : outputFormat;
        }
        imageValidationService.validateOutput(size, outputFormat, background);

        GenerationMode mode = mode(productImage, logoImage, referenceImage, maskImage, background);
        validateRequestForMode(request, mode);
        Instant startedAt = Instant.now();
        AiCreativePersistenceService.CreativeContext context = persistenceService.context(request);
        String promptTitle = promptTitleService.createTitle(context.brandName(), context.productServiceName(), context.campaignName(), request.creativeType(), request.platform());
        String prompt = promptBuilderService.buildFinalImagePrompt(request, mode, backgroundPrompt, context, size);
        Map<String, Object> plan = planningPlan(request, mode, promptTitle, prompt, size, quality, outputFormat, background);
        UUID promptRequestId = persistenceService.createPromptRequest(request, context, promptTitle, mode, size, quality, outputFormat, background, prompt, plan);
        UUID jobId = persistenceService.createJob(promptRequestId, request, promptTitle, mode, plan, openAiImageClient.model());
        persistenceService.createLayers(jobId, mode, openAiImageClient.model(), plan);

        try {
            persistenceService.markLayerProcessing(jobId, "REQUEST_ANALYSIS");
            persistenceService.markLayerCompleted(jobId, "REQUEST_ANALYSIS", Map.of("generationMode", storageMode(mode)));
            persistenceService.markLayerProcessing(jobId, "PROMPT_GENERATION");
            persistenceService.markLayerCompleted(jobId, "PROMPT_GENERATION", Map.of("promptTitle", promptTitle));
            persistenceService.markLayerProcessing(jobId, imageLayerKey(mode));

            OpenAiImageResponse openAiResponse = callOpenAi(request, productImage, logoImage, referenceImage, maskImage, prompt, size, quality, outputFormat, background, mode);
            persistenceService.markLayerCompleted(jobId, imageLayerKey(mode), Map.of("provider", "OPENAI", "model", openAiImageClient.model()));

            persistenceService.markLayerProcessing(jobId, "IMAGE_DECODE");
            String b64 = openAiResponse.data() == null || openAiResponse.data().isEmpty() ? null : openAiResponse.data().getFirst().b64Json();
            byte[] image = decoderService.decodeImage(b64);
            persistenceService.markLayerCompleted(jobId, "IMAGE_DECODE", Map.of("bytes", image.length));

            persistenceService.markLayerProcessing(jobId, "R2_UPLOAD");
            StorageService.StoredObject stored = storageService.storeGenerated(new StorageService.GeneratedStorageUploadRequest(
                    request.workspaceId(),
                    request.campaignId(),
                    jobId,
                    outputFormat.extension(),
                    outputFormat.contentType(),
                    image));
            String fileUrl = firstNonBlank(stored.publicUrl(), stored.previewUrl(), stored.thumbnailUrl());
            persistenceService.markLayerCompleted(jobId, "R2_UPLOAD", Map.of("r2ObjectKey", stored.storageKey()));
            persistenceService.completePromptRequest(promptRequestId, prompt);
            persistenceService.completeJob(jobId, stored.storageKey(), fileUrl, BigDecimal.ONE);
            return new AiCreativeResponse(
                    jobId,
                    request.workspaceId(),
                    request.brandId(),
                    request.campaignId(),
                    CreativeStatus.COMPLETED,
                    mode,
                    "OPENAI",
                    openAiImageClient.model(),
                    size,
                    quality.name(),
                    outputFormat,
                    background,
                    fileUrl,
                    stored.storageKey(),
                    BigDecimal.ONE,
                    BigDecimal.ONE,
                    startedAt,
                    Instant.now());
        } catch (RuntimeException exception) {
            persistenceService.failJob(jobId, safeFailure(exception));
            persistenceService.failPromptRequest(promptRequestId, safeFailure(exception));
            throw exception;
        }
    }

    public AiCreativeResponse generateText(TextCreativeRequest request) {
        return generate(new AiCreativeGenerateRequest(
                request.workspaceId(),
                request.brandId(),
                request.productServiceId(),
                request.campaignId(),
                request.platform(),
                request.language(),
                request.creativeType(),
                request.outputFormat(),
                request.tone(),
                request.modelQuality(),
                request.productDescription(),
                request.headline(),
                null,
                null,
                request.cta(),
                request.campaignObjective(),
                request.targetAudience(),
                request.productDescription(),
                1,
                null,
                request.noHumanModel(),
                request.size(),
                request.quality(),
                request.background(),
                null,
                null), null, null, null, null, null);
    }

    public Map<String, Object> progress(UUID creativeId) {
        return persistenceService.progress(creativeId);
    }

    private OpenAiImageResponse callOpenAi(
            AiCreativeGenerateRequest request,
            MultipartFile productImage,
            MultipartFile logoImage,
            MultipartFile referenceImage,
            MultipartFile maskImage,
            String prompt,
            String size,
            CreativeQuality quality,
            OutputFormat outputFormat,
            String background,
            GenerationMode mode
    ) {
        if (mode == GenerationMode.TEXT_TO_CREATIVE) {
            return openAiImageClient.generateImage(prompt, size, quality, outputFormat, background);
        }
        return openAiImageClient.editImage(prompt, size, quality, outputFormat, background, productImage, logoImage, referenceImage, maskImage);
    }

    private String prompt(AiCreativeGenerateRequest request, GenerationMode mode, String backgroundPrompt) {
        return switch (mode) {
            case TEXT_TO_CREATIVE -> promptBuilderService.buildTextCreativePrompt(request);
            case PRODUCT_IMAGE_TO_CREATIVE -> promptBuilderService.buildProductImageCreativePrompt(request);
            case MULTI_REFERENCE -> promptBuilderService.buildMultiReferenceCreativePrompt(request);
            case BACKGROUND_REPLACE -> promptBuilderService.buildBackgroundReplacementPrompt(request, backgroundPrompt);
            case TRANSPARENT_ASSET -> promptBuilderService.buildTransparentAssetPrompt(request);
        };
    }

    private GenerationMode mode(MultipartFile productImage, MultipartFile logoImage, MultipartFile referenceImage, MultipartFile maskImage, String background) {
        if (maskImage != null && !maskImage.isEmpty()) {
            if (productImage == null || productImage.isEmpty()) {
                throw new BusinessException(ErrorCode.GENERATION_VALIDATION_FAILED, "original/product image is required when maskImage is provided");
            }
            return GenerationMode.BACKGROUND_REPLACE;
        }
        if ("transparent".equalsIgnoreCase(background)) {
            return GenerationMode.TRANSPARENT_ASSET;
        }
        if (hasFile(logoImage) || hasFile(referenceImage)) {
            if (!hasFile(productImage)) {
                throw new BusinessException(ErrorCode.GENERATION_VALIDATION_FAILED, "productImage is required for multi-reference creative generation");
            }
            return GenerationMode.MULTI_REFERENCE;
        }
        return hasFile(productImage) ? GenerationMode.PRODUCT_IMAGE_TO_CREATIVE : GenerationMode.TEXT_TO_CREATIVE;
    }

    private void validateRequestForMode(AiCreativeGenerateRequest request, GenerationMode mode) {
        if (request.workspaceId() == null) {
            throw new BusinessException(ErrorCode.GENERATION_VALIDATION_FAILED, "workspaceId is required");
        }
        if (request.brandId() == null) {
            throw new BusinessException(ErrorCode.GENERATION_VALIDATION_FAILED, "brandId is required");
        }
        if (request.platform() == null) {
            throw new BusinessException(ErrorCode.GENERATION_VALIDATION_FAILED, "platform is required");
        }
        if (request.creativeType() == null) {
            throw new BusinessException(ErrorCode.GENERATION_VALIDATION_FAILED, "creativeType is required");
        }
        if (request.language() == null || request.language().isBlank()) {
            throw new BusinessException(ErrorCode.GENERATION_VALIDATION_FAILED, "language is required");
        }
        if (request.tone() == null) {
            throw new BusinessException(ErrorCode.GENERATION_VALIDATION_FAILED, "tone is required");
        }
        if (mode == GenerationMode.TEXT_TO_CREATIVE
                && (request.campaignIdea() == null || request.campaignIdea().isBlank())
                && (request.headline() == null || request.headline().isBlank())
                && (request.productDescription() == null || request.productDescription().isBlank())) {
            throw new BusinessException(
                    ErrorCode.GENERATION_VALIDATION_FAILED,
                    "Campaign idea is required for text-only creative generation.");
        }
    }

    private MultipartFile resolveExistingProductImage(AiCreativeGenerateRequest request, MultipartFile productImage) {
        if (hasFile(productImage) || request.existingAssetId() == null) {
            return productImage;
        }
        AssetEntity asset = assetRepository.findByIdAndWorkspaceIdAndDeletedFalse(request.existingAssetId(), request.workspaceId())
                .filter(AssetEntity::isReady)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASSET_NOT_FOUND, "Selected product image asset is not available"));
        byte[] bytes = storageService.readBytes(asset);
        return new ByteArrayMultipartFile(
                "productImage",
                asset.getOriginalFileName(),
                asset.getMimeType(),
                bytes);
    }

    private Map<String, Object> planningPlan(AiCreativeGenerateRequest request, GenerationMode mode, String promptTitle, String prompt, String size, CreativeQuality quality, OutputFormat outputFormat, String background) {
        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("strategy", "FULLY_SELF_EXECUTED");
        plan.put("generationMode", storageMode(mode));
        plan.put("primaryProvider", "OPENAI");
        plan.put("canPrimaryHandleAllLayers", true);
        plan.put("promptTitle", promptTitle);
        plan.put("finalPrompt", prompt);
        plan.put("size", size);
        plan.put("quality", quality.name());
        plan.put("outputFormat", outputFormat.name());
        plan.put("background", background);
        plan.put("layers", List.of(
                Map.of("sequence", 1, "layerKey", "REQUEST_ANALYSIS", "layerType", "REQUEST_ANALYSIS", "provider", "OPENAI", "model", "gpt-4.1-mini", "required", true, "estimatedCost", 0.05),
                Map.of("sequence", 2, "layerKey", "PROMPT_GENERATION", "layerType", "PROMPT_GENERATION", "provider", "OPENAI", "model", "gpt-4.1-mini", "required", true, "estimatedCost", 0.10),
                Map.of("sequence", 3, "layerKey", imageLayerKey(mode), "layerType", mode == GenerationMode.TEXT_TO_CREATIVE ? "IMAGE_GENERATION" : "IMAGE_EDIT", "provider", "OPENAI", "model", openAiImageClient.model(), "required", true, "estimatedCost", 1.30),
                Map.of("sequence", 4, "layerKey", "IMAGE_DECODE", "layerType", "INTERNAL_DECODE", "provider", "INTERNAL", "required", true, "estimatedCost", 0),
                Map.of("sequence", 5, "layerKey", "R2_UPLOAD", "layerType", "INTERNAL_SAVE", "provider", "INTERNAL", "required", true, "estimatedCost", 0)
        ));
        return plan;
    }

    private String imageLayerKey(GenerationMode mode) {
        return mode == GenerationMode.TEXT_TO_CREATIVE ? "IMAGE_GENERATION" : "IMAGE_EDIT";
    }

    private String storageMode(GenerationMode mode) {
        return switch (mode) {
            case TEXT_TO_CREATIVE -> "TEXT_ONLY_CREATIVE";
            case PRODUCT_IMAGE_TO_CREATIVE -> "PRODUCT_IMAGE_CREATIVE";
            case MULTI_REFERENCE -> "MULTI_REFERENCE_CREATIVE";
            case BACKGROUND_REPLACE -> "BACKGROUND_REPLACEMENT";
            case TRANSPARENT_ASSET -> "TRANSPARENT_ASSET";
        };
    }

    private String safeFailure(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "Creative generation failed. Please try again." : message;
    }

    private void validateFiles(MultipartFile productImage, MultipartFile logoImage, MultipartFile referenceImage, MultipartFile maskImage) {
        imageValidationService.validateImage(productImage, "productImage");
        imageValidationService.validateImage(logoImage, "logoImage");
        imageValidationService.validateImage(referenceImage, "referenceImage");
        imageValidationService.validateImage(maskImage, "maskImage");
    }

    private boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private String resolveSize(CreativeType type, String requested) {
        if (requested != null && !requested.isBlank()) {
            return requested.trim();
        }
        CreativeType effectiveType = type == null ? CreativeType.SQUARE_POST : type;
        return switch (effectiveType) {
            case STORY -> "1024x1536";
            case BANNER -> "1536x1024";
            case SQUARE_POST, PRODUCT_AD -> "1024x1024";
        };
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
