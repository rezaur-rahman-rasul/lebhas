package com.lebhas.creativesaas.generation.application;

import com.lebhas.creativesaas.asset.domain.AssetCategory;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.domain.AssetFileType;
import com.lebhas.creativesaas.asset.domain.AssetType;
import com.lebhas.creativesaas.asset.domain.StorageProvider;
import com.lebhas.creativesaas.asset.infrastructure.persistence.AssetRepository;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestStatus;
import com.lebhas.creativesaas.creativerequest.application.CreativeRequestValidationService;
import com.lebhas.creativesaas.creativerequest.infrastructure.persistence.CreativeRequestRepository;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.generatedversion.domain.GenerationStatus;
import com.lebhas.creativesaas.generatedversion.application.GeneratedVersionService;
import com.lebhas.creativesaas.generation.application.dto.CreditReservationResult;
import com.lebhas.creativesaas.generation.cache.GenerationLockService;
import com.lebhas.creativesaas.generation.domain.GenerationJobEntity;
import com.lebhas.creativesaas.generation.domain.GenerationJobStatus;
import com.lebhas.creativesaas.generation.event.GenerationCompletedEventDto;
import com.lebhas.creativesaas.generation.event.GenerationEventProducer;
import com.lebhas.creativesaas.generation.event.GenerationFailedEventDto;
import com.lebhas.creativesaas.generation.event.GenerationJobQueuedEventDto;
import com.lebhas.creativesaas.storage.application.StorageFileService;
import com.lebhas.creativesaas.storage.domain.StorageFileEntity;
import com.lebhas.creativesaas.redis.RedisLockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class GenerationWorkerService {

    private static final String CREDIT_REFERENCE_TYPE = "creative_request_generation";

    private final GenerationJobService generationJobService;
    private final CreativeRequestRepository creativeRequestRepository;
    private final GenerationExecutionContextFactory executionContextFactory;
    private final CreativeLayerPipelineExecutor creativeLayerPipelineExecutor;
    private final MockCreativeGenerationProvider mockCreativeGenerationProvider;
    private final StorageFileService storageFileService;
    private final AssetRepository assetRepository;
    private final GeneratedVersionService generatedVersionService;
    private final CreditReservationService creditReservationService;
    private final CreativeRequestValidationService creativeRequestValidationService;
    private final GenerationLockService generationLockService;
    private final GenerationEventProducer generationEventProducer;

    public GenerationWorkerService(
            GenerationJobService generationJobService,
            CreativeRequestRepository creativeRequestRepository,
            GenerationExecutionContextFactory executionContextFactory,
            CreativeLayerPipelineExecutor creativeLayerPipelineExecutor,
            MockCreativeGenerationProvider mockCreativeGenerationProvider,
            StorageFileService storageFileService,
            AssetRepository assetRepository,
            GeneratedVersionService generatedVersionService,
            CreditReservationService creditReservationService,
            CreativeRequestValidationService creativeRequestValidationService,
            GenerationLockService generationLockService,
            GenerationEventProducer generationEventProducer
    ) {
        this.generationJobService = generationJobService;
        this.creativeRequestRepository = creativeRequestRepository;
        this.executionContextFactory = executionContextFactory;
        this.creativeLayerPipelineExecutor = creativeLayerPipelineExecutor;
        this.mockCreativeGenerationProvider = mockCreativeGenerationProvider;
        this.storageFileService = storageFileService;
        this.assetRepository = assetRepository;
        this.generatedVersionService = generatedVersionService;
        this.creditReservationService = creditReservationService;
        this.creativeRequestValidationService = creativeRequestValidationService;
        this.generationLockService = generationLockService;
        this.generationEventProducer = generationEventProducer;
    }

    public void processQueuedJob(GenerationJobQueuedEventDto event) {
        RedisLockService.RedisLockToken lock = generationLockService
                .acquire(event.workspaceId(), event.creativeRequestId())
                .orElse(null);
        if (lock == null) {
            generationEventProducer.publishGenerationFailed(new GenerationFailedEventDto(
                    event.workspaceId(),
                    event.creativeRequestId(),
                    event.generationJobId(),
                    null,
                    event.creditReservationId(),
                    null,
                    "Generation is already running for this creative request",
                    true,
                    false,
                    Instant.now()));
            return;
        }
        try {
            run(event);
        } finally {
            generationLockService.release(lock, event.workspaceId(), event.creativeRequestId());
        }
    }

    @Transactional
    public void run(GenerationJobQueuedEventDto event) {
        GenerationJobEntity job = generationJobService.require(event.workspaceId(), event.generationJobId());
        if (job.getJobStatus() == GenerationJobStatus.COMPLETED) {
            return;
        }
        if (job.getJobStatus() != GenerationJobStatus.QUEUED) {
            throw new BusinessException(
                    ErrorCode.GENERATION_STATE_CONFLICT,
                    "Only queued generation jobs can be processed by the worker");
        }

        CreativeRequestEntity request = creativeRequestRepository
                .findByIdAndWorkspaceIdAndDeletedFalse(event.creativeRequestId(), event.workspaceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CREATIVE_REQUEST_FOUNDATION_NOT_FOUND));
        BigDecimal estimatedCost = creativeRequestValidationService.estimateCost(request);
        try {
            job = generationJobService.start(event.workspaceId(), event.generationJobId());
            request.markGenerationStarted(Instant.now());
            creativeRequestRepository.save(request);
            generationEventProducer.publishGenerationStarted(new com.lebhas.creativesaas.generation.event.GenerationStartedEventDto(
                    job.getWorkspaceId(),
                    job.getCreativeRequestId(),
                    job.getId(),
                    event.creditReservationId(),
                    job.getAttemptCount(),
                    Instant.now()));

            GenerationExecutionContext executionContext = executionContextFactory.create(job);
            Map<String, Object> layerOutputs = creativeLayerPipelineExecutor.execute(executionContext);
            List<GeneratedVersionEntity> versions = createGeneratedVersions(executionContext, layerOutputs);
            GeneratedVersionEntity latest = versions.isEmpty() ? null : versions.get(versions.size() - 1);
            UUID latestAssetId = latest == null ? null : latest.getAssetId();
            UUID latestStorageFileId = latest == null ? null : latest.getStorageFileId();

            if (event.creditReservationId() != null) {
                creditReservationService.finalize(
                        request.getWorkspaceId(),
                        request.getId(),
                        latest == null ? null : latest.getId(),
                        event.creditReservationId(),
                        CREDIT_REFERENCE_TYPE,
                        request.getId(),
                        estimatedCost,
                        "mock_generation_completed");
            }
            generationJobService.complete(event.workspaceId(), event.generationJobId(), "mock-completed-" + job.getId());
            request.markGenerationCompleted(Instant.now(), generatedVersionService.listByCreativeRequest(request.getWorkspaceId(), request.getId()).size());
            creativeRequestRepository.save(request);
            generationEventProducer.publishGenerationCompleted(new GenerationCompletedEventDto(
                    request.getWorkspaceId(),
                    request.getId(),
                    job.getId(),
                    latest == null ? null : latest.getId(),
                    latestAssetId,
                    latestStorageFileId,
                    null,
                    null,
                    event.creditReservationId(),
                    estimatedCost,
                    "MOCK_CREATIVE_PROVIDER",
                    "mock-creative-v1",
                    "mock-completed-" + job.getId(),
                    event.creditReservationId() != null,
                    Instant.now()));
        } catch (RuntimeException exception) {
            failAndRefund(event, request, estimatedCost, exception);
            throw exception;
        }
    }

    private List<GeneratedVersionEntity> createGeneratedVersions(
            GenerationExecutionContext executionContext,
            Map<String, Object> layerOutputs
    ) {
        int count = Math.max(1, executionContext.request().getRequestedVersions());
        java.util.ArrayList<GeneratedVersionEntity> versions = new java.util.ArrayList<>();
        for (int index = 0; index < count; index++) {
            int versionNumber = generatedVersionService.nextVersionNumber(
                    executionContext.request().getWorkspaceId(),
                    executionContext.request().getId());
            MockCreativeGenerationResult result = mockCreativeGenerationProvider.generate(executionContext, layerOutputs, versionNumber);
            StorageFileEntity file = storageFileService.registerGeneratedOutput(
                    executionContext.request().getWorkspaceId(),
                    executionContext.request().getProjectCampaignId(),
                    StorageProvider.R2,
                    "mock-generated",
                    result.objectKey(),
                    null,
                    result.mimeType(),
                    result.fileExtension(),
                    result.content().length,
                    result.width(),
                    result.height(),
                    result.duration(),
                    result.content());
            AssetEntity asset = AssetEntity.createSignedUploadPending(
                    executionContext.request().getWorkspaceId(),
                    executionContext.request().getBrandId(),
                    executionContext.request().getProductServiceId(),
                    executionContext.request().getProjectCampaignId(),
                    executionContext.request().getCreatedByUserId(),
                    null,
                    AssetType.GENERATED_CREATIVE,
                    result.fileExtension().equals("mp4") ? AssetCategory.EXPORT_VIDEO : AssetCategory.EXPORT_IMAGE,
                    "mock-generated-v" + versionNumber + "." + result.fileExtension(),
                    "mock-generated-v" + versionNumber + "." + result.fileExtension(),
                    "Generated Version " + versionNumber,
                    "Mock creative generation output metadata",
                    Set.of("generated", "mock"),
                    null,
                    metadataJson(result.metadata()),
                    StorageProvider.R2,
                    file.getBucket(),
                    file.getObjectKey(),
                    result.fileExtension().equals("mp4") ? AssetFileType.VIDEO : AssetFileType.IMAGE,
                    result.mimeType(),
                    result.fileExtension(),
                    result.content().length,
                    file.getHash(),
                    "mock-generation");
            asset.confirmSignedUpload(file.getId(), result.width(), result.height(), result.duration());
            asset = assetRepository.save(asset);

            GeneratedVersionEntity version = generatedVersionService.createCompleted(
                    executionContext.request(),
                    executionContext.request().getCreatedByUserId(),
                    versionNumber,
                    file.getId(),
                    asset.getId(),
                    null,
                    null,
                    result.providerName(),
                    result.model());
            version.recordGeneratedAsset(
                    asset.getId(),
                    null,
                    null,
                    1L,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    result.width(),
                    result.height(),
                    result.fileExtension());
            version.markReady(file.getId(), asset.getId(), result.providerName(), result.model());
            version = generatedVersionService.save(version);
            versions.add(version);
        }
        return versions;
    }

    private void failAndRefund(
            GenerationJobQueuedEventDto event,
            CreativeRequestEntity request,
            BigDecimal estimatedCost,
            RuntimeException exception
    ) {
        String reason = safeReason(exception);
        generationJobService.fail(event.workspaceId(), event.generationJobId(), reason);
        if (request.getStatus() != CreativeRequestStatus.COMPLETED) {
            request.markGenerationFailed(reason, Instant.now());
            creativeRequestRepository.save(request);
        }
        if (event.creditReservationId() != null) {
            creditReservationService.refund(
                    request.getWorkspaceId(),
                    request.getId(),
                    null,
                    event.creditReservationId(),
                    CREDIT_REFERENCE_TYPE,
                    request.getId(),
                    estimatedCost,
                    "mock_generation_failed");
        }
        generationEventProducer.publishGenerationFailed(new GenerationFailedEventDto(
                request.getWorkspaceId(),
                request.getId(),
                event.generationJobId(),
                null,
                event.creditReservationId(),
                event.creditReservationId() == null ? null : estimatedCost,
                reason,
                true,
                event.creditReservationId() != null,
                Instant.now()));
    }

    private String metadataJson(Map<String, Object> metadata) {
        Map<String, Object> normalized = new LinkedHashMap<>(metadata == null ? Map.of() : metadata);
        normalized.put("generatedBy", "MockCreativeGenerationProvider");
        return normalized.toString();
    }

    private String safeReason(Throwable exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        String normalized = message.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 1000 ? normalized : normalized.substring(0, 1000);
    }
}
