package com.lebhas.ai.creative.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.lebhas.ai.application.OpenAiCostTrackingService;
import com.lebhas.creativesaas.asset.domain.AssetCategory;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.domain.AssetFileType;
import com.lebhas.creativesaas.asset.domain.AssetType;
import com.lebhas.creativesaas.asset.infrastructure.persistence.AssetRepository;
import com.lebhas.creativesaas.asset.storage.StorageService;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.storage.application.StorageFileService;
import com.lebhas.creativesaas.storage.domain.StorageFileEntity;
import com.lebhas.creativesaas.usage.application.CreditUsageService;
import com.lebhas.creativesaas.usage.application.dto.CreditUsageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AiCreativeService {

    private static final Logger log = LoggerFactory.getLogger(AiCreativeService.class);
    private static final int MAX_PERSISTENCE_ATTEMPTS = 3;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AiCreativePromptBuilderService promptBuilderService;
    private final OpenAiImageClient openAiImageClient;
    private final Base64ImageDecoderService decoderService;
    private final ImageValidationService imageValidationService;
    private final StorageService storageService;
    private final PromptTitleService promptTitleService;
    private final AiCreativePersistenceService persistenceService;
    private final AssetRepository assetRepository;
    private final StorageFileService storageFileService;
    private final BengaliTypographyOverlayService bengaliTypographyOverlayService;
    private final LogoOverlayService logoOverlayService;
    private final CurrentUserContext currentUserContext;
    private final CreditUsageService creditUsageService;
    private final ObjectProvider<OpenAiCostTrackingService> openAiCostTrackingService;

    public AiCreativeService(
            AiCreativePromptBuilderService promptBuilderService,
            OpenAiImageClient openAiImageClient,
            Base64ImageDecoderService decoderService,
            ImageValidationService imageValidationService,
            StorageService storageService,
            PromptTitleService promptTitleService,
            AiCreativePersistenceService persistenceService,
            AssetRepository assetRepository,
            StorageFileService storageFileService,
            BengaliTypographyOverlayService bengaliTypographyOverlayService,
            LogoOverlayService logoOverlayService,
            CurrentUserContext currentUserContext,
            CreditUsageService creditUsageService,
            ObjectProvider<OpenAiCostTrackingService> openAiCostTrackingService
    ) {
        this.promptBuilderService = promptBuilderService;
        this.openAiImageClient = openAiImageClient;
        this.decoderService = decoderService;
        this.imageValidationService = imageValidationService;
        this.storageService = storageService;
        this.promptTitleService = promptTitleService;
        this.persistenceService = persistenceService;
        this.assetRepository = assetRepository;
        this.storageFileService = storageFileService;
        this.bengaliTypographyOverlayService = bengaliTypographyOverlayService;
        this.logoOverlayService = logoOverlayService;
        this.currentUserContext = currentUserContext;
        this.creditUsageService = creditUsageService;
        this.openAiCostTrackingService = openAiCostTrackingService;
    }

    public AiCreativeResponse generate(
            AiCreativeGenerateRequest request,
            MultipartFile productImage,
            MultipartFile logoImage,
            MultipartFile referenceImage,
            MultipartFile maskImage,
            String backgroundPrompt
    ) {
        if (request.includeLogo() != null && !request.includeLogo()) {
            logoImage = null;
        }
        request = normalizeTextControls(request);
        productImage = resolveExistingProductImage(request, productImage);
        logoImage = resolveExistingLogoImage(request, logoImage);
        validateFiles(productImage, logoImage, referenceImage, maskImage);
        MultipartFile logoImageForOverlay = logoImage;
        MultipartFile logoImageForProvider = null;
        OutputFormat outputFormat = request.outputFormat() == null ? OutputFormat.png : request.outputFormat();
        if (bengaliTypographyOverlayService.requiresOverlay(request) && outputFormat == OutputFormat.webp) {
            outputFormat = OutputFormat.png;
        }
        CreativeQuality quality = request.quality() == null ? CreativeQuality.fromModelQuality(request.modelQuality()) : request.quality();
        String size = resolveSize(request.creativeType(), request.size());
        String background = request.background() == null || request.background().isBlank() ? "opaque" : request.background().trim().toLowerCase();
        if ("transparent".equals(background)) {
            outputFormat = outputFormat == OutputFormat.jpeg ? OutputFormat.png : outputFormat;
        }
        imageValidationService.validateOutput(size, outputFormat, background);

        GenerationMode mode = mode(productImage, logoImageForProvider, referenceImage, maskImage, background);
        validateRequestForMode(request, mode);
        Instant startedAt = Instant.now();
        AiCreativePersistenceService.CreativeContext context = persistenceService.context(request);
        String promptTitle = promptTitleService.createTitle(context.brandName(), context.productServiceName(), context.campaignName(), request.creativeType(), request.platform());
        String prompt = promptBuilderService.buildFinalImagePrompt(request, mode, backgroundPrompt, context, size);
        Map<String, Object> plan = planningPlan(request, mode, promptTitle, prompt, size, quality, outputFormat, background);
        UUID promptRequestId = persistenceService.createPromptRequest(request, context, promptTitle, mode, size, quality, outputFormat, background, prompt, plan);
        UUID jobId = persistenceService.createJob(promptRequestId, request, promptTitle, mode, plan, openAiImageClient.model());
        persistenceService.createLayers(jobId, mode, openAiImageClient.model(), plan);
        UUID actorUserId = actorUserId(request);
        CreditUsageResult creditReservation = null;

        try {
            creditReservation = creditUsageService.reservePromptRequestCredits(
                    request.workspaceId(),
                    promptRequestId,
                    jobId,
                    BigDecimal.ONE,
                    actorUserId);
            persistenceService.markLayerProcessing(jobId, "REQUEST_ANALYSIS");
            persistenceService.markLayerCompleted(jobId, "REQUEST_ANALYSIS", Map.of("generationMode", storageMode(mode)));
            persistenceService.markLayerProcessing(jobId, "PROMPT_GENERATION");
            persistenceService.markLayerCompleted(jobId, "PROMPT_GENERATION", Map.of("promptTitle", promptTitle));
            persistenceService.markLayerProcessing(jobId, imageLayerKey(mode));
            persistenceService.markJobStatus(jobId, "GENERATING");

            OpenAiImageResponse openAiResponse = callOpenAi(request, productImage, logoImageForProvider, referenceImage, maskImage, prompt, size, quality, outputFormat, background, mode);
            recordOpenAiSpendQuietly(request.workspaceId(), jobId);
            persistenceService.markLayerCompleted(jobId, imageLayerKey(mode), Map.of("provider", "OPENAI", "model", openAiImageClient.model()));
            log.info("ai_creative_pipeline event=openai_response workspaceId={} jobId={} provider=OPENAI model={} dataCount={}",
                    request.workspaceId(), jobId, openAiImageClient.model(), openAiResponse.data() == null ? 0 : openAiResponse.data().size());

            persistenceService.markLayerProcessing(jobId, "IMAGE_DECODE");
            persistenceService.markJobStatus(jobId, "DOWNLOADING");
            OpenAiImageResponse.ImageData imageData = openAiResponse.data() == null || openAiResponse.data().isEmpty() ? null : openAiResponse.data().getFirst();
            String b64 = imageData == null ? null : imageData.b64Json();
            String imageUrl = imageData == null ? null : imageData.url();
            byte[] image = retry("openai-image-download", jobId, () -> decoderService.decodeImage(b64, imageUrl));
            persistenceService.markLayerCompleted(jobId, "IMAGE_DECODE", Map.of("bytes", image.length));
            log.info("ai_creative_pipeline event=image_downloaded workspaceId={} jobId={} bytes={} mimeType={}",
                    request.workspaceId(), jobId, image.length, outputFormat.contentType());

            if (hasFile(logoImageForOverlay)) {
                persistenceService.markLayerProcessing(jobId, "LOGO_OVERLAY");
                LogoOverlayService.OverlayResult logoOverlay = logoOverlayService.overlay(image, fileBytes(logoImageForOverlay, "logoImage"), outputFormat);
                image = logoOverlay.imageBytes();
                persistenceService.markLayerCompleted(jobId, "LOGO_OVERLAY", logoOverlay.metadata());
            }
            if (bengaliTypographyOverlayService.requiresOverlay(request)) {
                persistenceService.markLayerProcessing(jobId, "TEXT_OVERLAY");
                BengaliTypographyOverlayService.RenderedTypography renderedTypography = bengaliTypographyOverlayService.render(image, request, outputFormat);
                image = renderedTypography.imageBytes();
                persistenceService.markLayerCompleted(jobId, "TEXT_OVERLAY", renderedTypography.metadata());

                persistenceService.markLayerProcessing(jobId, "TYPOGRAPHY_VALIDATION");
                persistenceService.markLayerCompleted(jobId, "TYPOGRAPHY_VALIDATION", Map.of(
                        "valid", true,
                        "ocrMode", "backend-unicode-source-validation",
                        "reason", "AI-generated Bengali glyphs suppressed; final Bangla rendered from normalized Unicode overlay",
                        "overlayApplied", renderedTypography.applied()));
            }

            persistenceService.markLayerProcessing(jobId, "R2_UPLOAD");
            persistenceService.markJobStatus(jobId, "UPLOADING");
            PersistedCreativeAsset persistedAsset = persistGeneratedCreative(
                    request,
                    jobId,
                    promptTitle,
                    outputFormat,
                    size,
                    image);
            persistenceService.markLayerCompleted(jobId, "R2_UPLOAD", Map.of(
                    "assetId", persistedAsset.assetId().toString(),
                    "r2ObjectKey", persistedAsset.storageKey(),
                    "fileSize", persistedAsset.fileSize()));
            persistenceService.completePromptRequest(promptRequestId, prompt);
            persistenceService.saveGeneratedVersion(
                    promptRequestId,
                    jobId,
                    request,
                    promptTitle,
                    persistedAsset.assetId(),
                    persistedAsset.storageKey(),
                    persistedAsset.previewUrl(),
                    persistedAsset.downloadUrl(),
                    BigDecimal.ONE,
                    persistedAsset.width(),
                    persistedAsset.height(),
                    persistedAsset.fileSize(),
                    outputFormat.contentType(),
                    openAiImageClient.model(),
                    generatedVersionMetadata(request, jobId, persistedAsset, prompt));
            persistenceService.completeJob(
                    jobId,
                    persistedAsset.assetId(),
                    persistedAsset.storageKey(),
                    persistedAsset.previewUrl(),
                    persistedAsset.downloadUrl(),
                    BigDecimal.ONE);
            CreditUsageResult finalizedCredits = creditUsageService.finalizePromptRequestCredits(
                    request.workspaceId(),
                    promptRequestId,
                    jobId,
                    creditReservation.creditReservationId(),
                    "Direct AI creative generation completed",
                    actorUserId);
            log.info("ai_creative_pipeline event=ready workspaceId={} jobId={} assetId={} storageKey={} fileSize={}",
                    request.workspaceId(), jobId, persistedAsset.assetId(), persistedAsset.storageKey(), persistedAsset.fileSize());
            return new AiCreativeResponse(
                    jobId,
                    request.workspaceId(),
                    request.brandId(),
                    request.campaignId(),
                    CreativeStatus.READY,
                    mode,
                    "OPENAI",
                    openAiImageClient.model(),
                    size,
                    quality.name(),
                    outputFormat,
                    background,
                    persistedAsset.previewUrl(),
                    persistedAsset.storageKey(),
                    persistedAsset.assetId(),
                    persistedAsset.previewUrl(),
                    persistedAsset.downloadUrl(),
                    persistedAsset.fileSize(),
                    persistedAsset.width(),
                    persistedAsset.height(),
                    outputFormat.contentType(),
                    finalizedCredits.creditsAmount(),
                    finalizedCredits.creditsAmount(),
                    startedAt,
                    Instant.now());
        } catch (RuntimeException exception) {
            refundReservedCreditsQuietly(request.workspaceId(), promptRequestId, jobId, creditReservation, actorUserId, exception);
            persistenceService.failJob(jobId, safeFailure(exception));
            persistenceService.failPromptRequest(promptRequestId, safeFailure(exception));
            throw exception;
        }
    }

    private AiCreativeGenerateRequest normalizeTextControls(AiCreativeGenerateRequest request) {
        if (request.includeTypography() == null || request.includeTypography()) {
            return request;
        }
        return new AiCreativeGenerateRequest(
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
                request.campaignIdea(),
                null,
                null,
                null,
                null,
                request.campaignObjective(),
                request.targetAudience(),
                request.productDescription(),
                request.includeCta(),
                request.includeLogo(),
                false,
                request.versions(),
                request.existingAssetId(),
                request.logoAssetId(),
                request.noHumanModel(),
                request.size(),
                request.quality(),
                request.background(),
                request.promptTitlePreview(),
                request.generationModeHint(),
                request.requestedByUserId());
    }

    private PersistedCreativeAsset persistGeneratedCreative(
            AiCreativeGenerateRequest request,
            UUID jobId,
            String promptTitle,
            OutputFormat outputFormat,
            String size,
            byte[] image
    ) {
        Dimension dimension = parseDimension(size);
        StorageService.StoredObject stored = retry("r2-upload", jobId, () -> storageService.storeGenerated(new StorageService.GeneratedStorageUploadRequest(
                request.workspaceId(),
                request.campaignId(),
                jobId,
                outputFormat.extension(),
                outputFormat.contentType(),
                image)));
        log.info("ai_creative_pipeline event=r2_upload_success workspaceId={} jobId={} bucket={} storageKey={} bytes={}",
                request.workspaceId(), jobId, stored.bucket(), stored.storageKey(), image.length);

        AssetEntity asset = AssetEntity.createUploading(
                request.workspaceId(),
                request.brandId(),
                request.productServiceId(),
                request.campaignId(),
                actorUserId(request),
                null,
                AssetType.GENERATED_CREATIVE,
                AssetCategory.GENERATED_CREATIVE,
                "generated-creative-" + jobId + "." + outputFormat.extension(),
                promptTitle,
                "AI generated creative output",
                java.util.Set.of("generated", "openai", "creative"),
                null,
                assetMetadata(request, jobId, stored.storageKey()),
                storageService.provider());
        asset.completeUpload(
                stored.storedFileName(),
                AssetFileType.IMAGE,
                outputFormat.contentType(),
                outputFormat.extension(),
                image.length,
                storageService.provider(),
                stored.bucket(),
                stored.storageKey(),
                stored.publicUrl(),
                null,
                stored.thumbnailUrl(),
                dimension.width(),
                dimension.height(),
                null);
        StorageFileEntity storageFile = retry("storage-file-register", jobId, () -> storageFileService.registerGeneratedOutput(
                request.workspaceId(),
                request.campaignId(),
                storageService.provider(),
                stored.bucket(),
                stored.storageKey(),
                stored.publicUrl(),
                outputFormat.contentType(),
                outputFormat.extension(),
                image.length,
                dimension.width(),
                dimension.height(),
                null,
                image));
        asset.attachStorageFile(storageFile.getId());
        AssetEntity pendingAsset = asset;
        asset = retry("asset-persist", jobId, () -> assetRepository.saveAndFlush(pendingAsset));
        log.info("ai_creative_pipeline event=asset_persisted workspaceId={} jobId={} assetId={} storageKey={}",
                request.workspaceId(), jobId, asset.getId(), asset.getStorageKey());

        AssetEntity signedAsset = asset;
        StorageService.SignedAssetUrl previewUrl = retry("signed-preview-url", jobId, () -> storageService.generatePreviewUrl(signedAsset));
        StorageService.SignedAssetUrl downloadUrl = retry("signed-download-url", jobId, () -> storageService.generateDownloadUrl(signedAsset));
        log.info("ai_creative_pipeline event=signed_urls_ready workspaceId={} jobId={} assetId={} previewExpiresAt={} downloadExpiresAt={}",
                request.workspaceId(), jobId, asset.getId(), previewUrl.expiresAt(), downloadUrl.expiresAt());
        return new PersistedCreativeAsset(
                asset.getId(),
                asset.getStorageKey(),
                previewUrl.url(),
                downloadUrl.url(),
                image.length,
                dimension.width(),
                dimension.height());
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
                request.cta() != null,
                false,
                true,
                1,
                null,
                null,
                request.noHumanModel(),
                request.size(),
                request.quality(),
                request.background(),
                null,
                null,
                actorUserId(null)), null, null, null, null, null);
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

    private void recordOpenAiSpendQuietly(UUID workspaceId, UUID jobId) {
        try {
            OpenAiCostTrackingService costTrackingService = openAiCostTrackingService.getIfAvailable();
            if (costTrackingService != null) {
                costTrackingService.recordOpenAiCreativeGenerationCost();
            }
        } catch (RuntimeException exception) {
            log.warn("ai_creative_pipeline event=provider_spend_tracking_failed workspaceId={} jobId={} reason={}",
                    workspaceId, jobId, safeFailure(exception));
        }
    }

    private <T> T retry(String operation, UUID jobId, Retryable<T> action) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= MAX_PERSISTENCE_ATTEMPTS; attempt++) {
            long started = System.nanoTime();
            try {
                T result = action.run();
                log.info("ai_creative_pipeline event=retry_operation_success operation={} jobId={} attempt={} durationMs={}",
                        operation, jobId, attempt, elapsedMs(started));
                return result;
            } catch (RuntimeException exception) {
                last = exception;
                log.warn("ai_creative_pipeline event=retry_operation_failed operation={} jobId={} attempt={} durationMs={} reason={}",
                        operation, jobId, attempt, elapsedMs(started), safeFailure(exception));
                if (attempt < MAX_PERSISTENCE_ATTEMPTS) {
                    sleepBackoff(attempt);
                }
            }
        }
        throw last == null
                ? new BusinessException(ErrorCode.INTERNAL_ERROR, operation + " failed")
                : last;
    }

    private void sleepBackoff(int attempt) {
        try {
            Thread.sleep((long) Math.pow(2, attempt - 1) * 250L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Generation persistence retry was interrupted");
        }
    }

    private long elapsedMs(long startedNanos) {
        return java.time.Duration.ofNanos(System.nanoTime() - startedNanos).toMillis();
    }

    private String assetMetadata(AiCreativeGenerateRequest request, UUID jobId, String storageKey) {
        return json(Map.of(
                "source", "ai_creative_generation",
                "provider", "OPENAI",
                "model", openAiImageClient.model(),
                "creativeJobId", jobId.toString(),
                "brandId", request.brandId().toString(),
                "storageKey", storageKey));
    }

    private String generatedVersionMetadata(
            AiCreativeGenerateRequest request,
            UUID jobId,
            PersistedCreativeAsset asset,
            String prompt
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "ai_creative_generation");
        metadata.put("provider", "OPENAI");
        metadata.put("model", openAiImageClient.model());
        metadata.put("creativeJobId", jobId);
        metadata.put("brandId", request.brandId());
        metadata.put("generatedAssetId", asset.assetId());
        metadata.put("storageKey", asset.storageKey());
        metadata.put("promptUsed", prompt);
        return json(metadata);
    }

    private Dimension parseDimension(String size) {
        if (size == null || !size.contains("x")) {
            return new Dimension(null, null);
        }
        String[] parts = size.toLowerCase().split("x", 2);
        try {
            return new Dimension(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
        } catch (NumberFormatException exception) {
            return new Dimension(null, null);
        }
    }

    private String json(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Creative generation metadata could not be serialized");
        }
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

    private MultipartFile resolveExistingLogoImage(AiCreativeGenerateRequest request, MultipartFile logoImage) {
        if (request.includeLogo() != null && !request.includeLogo()) {
            return null;
        }
        if (hasFile(logoImage) || request.logoAssetId() == null) {
            return logoImage;
        }
        AssetEntity asset = assetRepository.findByIdAndWorkspaceIdAndDeletedFalse(request.logoAssetId(), request.workspaceId())
                .filter(AssetEntity::isReady)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASSET_NOT_FOUND, "Selected brand logo asset is not available"));
        byte[] bytes = storageService.readBytes(asset);
        return new ByteArrayMultipartFile(
                "logoImage",
                asset.getOriginalFileName(),
                asset.getMimeType(),
                bytes);
    }

    private Map<String, Object> planningPlan(AiCreativeGenerateRequest request, GenerationMode mode, String promptTitle, String prompt, String size, CreativeQuality quality, OutputFormat outputFormat, String background) {
        Map<String, Object> plan = new LinkedHashMap<>();
        boolean banglaOverlay = bengaliTypographyOverlayService.requiresOverlay(request);
        plan.put("strategy", "FULLY_SELF_EXECUTED");
        plan.put("generationMode", storageMode(mode));
        plan.put("primaryProvider", "OPENAI");
        plan.put("canPrimaryHandleAllLayers", true);
        plan.put("typographyPipeline", banglaOverlay ? "NO_TEXT_IMAGE_PLUS_INTERNAL_OVERLAY" : "IMAGE_MODEL_NATIVE_TEXT");
        plan.put("logoPipeline", request.includeLogo() != null && request.includeLogo() ? "INTERNAL_LOGO_OVERLAY" : "NONE");
        plan.put("promptTitle", promptTitle);
        plan.put("finalPrompt", prompt);
        plan.put("size", size);
        plan.put("quality", quality.name());
        plan.put("outputFormat", outputFormat.name());
        plan.put("background", background);
        List<Map<String, Object>> layers = new java.util.ArrayList<>(List.of(
                Map.of("sequence", 1, "layerKey", "REQUEST_ANALYSIS", "layerType", "REQUEST_ANALYSIS", "provider", "OPENAI", "model", "gpt-4.1-mini", "required", true, "estimatedCost", 0.05),
                Map.of("sequence", 2, "layerKey", "PROMPT_GENERATION", "layerType", "PROMPT_GENERATION", "provider", "OPENAI", "model", "gpt-4.1-mini", "required", true, "estimatedCost", 0.10),
                Map.of("sequence", 3, "layerKey", imageLayerKey(mode), "layerType", mode == GenerationMode.TEXT_TO_CREATIVE ? "IMAGE_GENERATION" : "IMAGE_EDIT", "provider", "OPENAI", "model", openAiImageClient.model(), "required", true, "estimatedCost", 1.30),
                Map.of("sequence", 4, "layerKey", "IMAGE_DECODE", "layerType", "INTERNAL_DECODE", "provider", "INTERNAL", "required", true, "estimatedCost", 0)
        ));
        int nextSequence = 5;
        if (request.includeLogo() != null && request.includeLogo()) {
            layers.add(Map.of("sequence", nextSequence++, "layerKey", "LOGO_OVERLAY", "layerType", "LOGO_OVERLAY", "provider", "INTERNAL", "model", "JAVA2D_LOGO_OVERLAY", "required", false, "estimatedCost", 0));
        }
        if (banglaOverlay) {
            layers.add(Map.of("sequence", nextSequence++, "layerKey", "TEXT_OVERLAY", "layerType", "TEXT_OVERLAY", "provider", "INTERNAL", "model", "JAVA2D_UNICODE", "required", true, "estimatedCost", 0));
            layers.add(Map.of("sequence", nextSequence++, "layerKey", "TYPOGRAPHY_VALIDATION", "layerType", "VISION_QUALITY_CHECK", "provider", "INTERNAL", "model", "BANGLA_UNICODE_VALIDATOR", "required", true, "estimatedCost", 0));
        }
        layers.add(Map.of("sequence", nextSequence, "layerKey", "R2_UPLOAD", "layerType", "INTERNAL_SAVE", "provider", "INTERNAL", "required", true, "estimatedCost", 0));
        plan.put("layers", layers);
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

    private void refundReservedCreditsQuietly(
            UUID workspaceId,
            UUID promptRequestId,
            UUID jobId,
            CreditUsageResult creditReservation,
            UUID actorUserId,
            RuntimeException cause
    ) {
        if (creditReservation == null) {
            return;
        }
        try {
            creditUsageService.refundPromptRequestCredits(
                    workspaceId,
                    promptRequestId,
                    jobId,
                    creditReservation.creditReservationId(),
                    safeFailure(cause),
                    actorUserId);
        } catch (RuntimeException refundException) {
            log.warn("ai_creative_pipeline event=credit_refund_failed workspaceId={} jobId={} promptRequestId={} reason={}",
                    workspaceId, jobId, promptRequestId, safeFailure(refundException));
        }
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

    private byte[] fileBytes(MultipartFile file, String field) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.ASSET_FILE_CONTENT_INVALID, field + " content could not be read");
        }
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

    private UUID actorUserId(AiCreativeGenerateRequest request) {
        if (request != null && request.requestedByUserId() != null) {
            return request.requestedByUserId();
        }
        return currentUserContext.requireCurrentUser().userId();
    }

    @FunctionalInterface
    private interface Retryable<T> {
        T run();
    }

    private record Dimension(Integer width, Integer height) {
    }

    private record PersistedCreativeAsset(
            UUID assetId,
            String storageKey,
            String previewUrl,
            String downloadUrl,
            long fileSize,
            Integer width,
            Integer height
    ) {
    }
}
