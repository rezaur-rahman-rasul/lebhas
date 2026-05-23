package com.lebhas.creativesaas.generation.application;

import com.lebhas.creativesaas.asset.application.AssetActivityLogger;
import com.lebhas.creativesaas.asset.domain.AssetType;
import com.lebhas.creativesaas.asset.domain.AssetCategory;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.domain.AssetFileType;
import com.lebhas.creativesaas.asset.infrastructure.persistence.AssetRepository;
import com.lebhas.creativesaas.asset.storage.StorageService;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.storage.application.StorageFileService;
import com.lebhas.creativesaas.generation.domain.CreativeGenerationRequestEntity;
import com.lebhas.creativesaas.generation.domain.CreativeGenerationStatus;
import com.lebhas.creativesaas.generation.domain.CreativeOutputEntity;
import com.lebhas.creativesaas.generation.domain.GenerationJobEntity;
import com.lebhas.creativesaas.generation.infrastructure.persistence.CreativeGenerationRequestRepository;
import com.lebhas.creativesaas.generation.infrastructure.persistence.CreativeOutputRepository;
import com.lebhas.creativesaas.generation.infrastructure.persistence.GenerationJobRepository;
import com.lebhas.creativesaas.generation.provider.AiGenerationResponse;
import com.lebhas.creativesaas.generation.queue.GenerationJobEventPublisher;
import com.lebhas.creativesaas.generation.queue.GenerationJobQueuedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class GenerationJobStateService {

    private final GenerationJobRepository jobRepository;
    private final CreativeGenerationRequestRepository requestRepository;
    private final CreativeOutputRepository outputRepository;
    private final AssetRepository assetRepository;
    private final StorageService storageService;
    private final GenerationJsonCodec jsonCodec;
    private final CreativeGenerationActivityLogger activityLogger;
    private final AssetActivityLogger assetActivityLogger;
    private final CreativeGenerationCreditService creditService;
    private final GenerationJobEventPublisher jobEventPublisher;
    private final StorageFileService storageFileService;

    public GenerationJobStateService(
            GenerationJobRepository jobRepository,
            CreativeGenerationRequestRepository requestRepository,
            CreativeOutputRepository outputRepository,
            AssetRepository assetRepository,
            StorageService storageService,
            GenerationJsonCodec jsonCodec,
            CreativeGenerationActivityLogger activityLogger,
            AssetActivityLogger assetActivityLogger,
            CreativeGenerationCreditService creditService,
            GenerationJobEventPublisher jobEventPublisher,
            StorageFileService storageFileService
    ) {
        this.jobRepository = jobRepository;
        this.requestRepository = requestRepository;
        this.outputRepository = outputRepository;
        this.assetRepository = assetRepository;
        this.storageService = storageService;
        this.jsonCodec = jsonCodec;
        this.activityLogger = activityLogger;
        this.assetActivityLogger = assetActivityLogger;
        this.creditService = creditService;
        this.jobEventPublisher = jobEventPublisher;
        this.storageFileService = storageFileService;
    }

    @Transactional
    public GenerationWorkItem start(UUID jobId) {
        GenerationJobEntity job = requireJob(jobId);
        CreativeGenerationRequestEntity request = requireRequest(job.getWorkspaceId(), job.getRequestId());
        if (job.getStatus() != CreativeGenerationStatus.QUEUED) {
            return null;
        }
        if (request.getStatus() == CreativeGenerationStatus.CANCELLED) {
            job.markCancelled("Request was cancelled before processing");
            jobRepository.save(job);
            return null;
        }
        job.markStarted();
        request.markProcessing();
        jobRepository.save(job);
        requestRepository.save(request);
        activityLogger.logJobStarted(job.getWorkspaceId(), request.getId(), job.getId(), job.getAttemptCount());
        return toWorkItem(job, request);
    }

    @Transactional
    public void complete(UUID jobId, AiGenerationResponse response) {
        GenerationJobEntity job = requireJob(jobId);
        CreativeGenerationRequestEntity request = requireRequest(job.getWorkspaceId(), job.getRequestId());
        if (request.getStatus() == CreativeGenerationStatus.CANCELLED) {
            job.markCancelled("Request was cancelled while provider generation was running");
            jobRepository.save(job);
            creditService.releaseReservedCredits(request);
            return;
        }
        if (response.content().length == 0) {
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_RESPONSE_INVALID, "Provider returned an empty creative artifact");
        }

        CreativeOutputEntity output = CreativeOutputEntity.processing(
                request.getWorkspaceId(),
                request.getProjectId(),
                request.getId(),
                request.getCreativeType(),
                request.getPlatform(),
                response.outputFormat(),
                response.width() == null ? requestDimensionWidth(request) : response.width(),
                response.height() == null ? requestDimensionHeight(request) : response.height(),
                response.duration());
        outputRepository.saveAndFlush(output);

        StorageService.StoredObject storedObject = storageService.storeGenerated(new StorageService.GeneratedStorageUploadRequest(
                request.getWorkspaceId(),
                request.getProjectId(),
                output.getId(),
                response.outputFormat().extension(),
                response.mimeType(),
                response.content()));

        AssetEntity asset = AssetEntity.createUploading(
                request.getWorkspaceId(),
                null,
                null,
                request.getProjectId(),
                request.getUserId(),
                null,
                AssetType.GENERATED,
                AssetCategory.OTHER,
                "generated-" + output.getId() + "." + response.outputFormat().extension(),
                "generated-" + output.getId() + "." + response.outputFormat().extension(),
                null,
                generatedTags(request),
                null,
                generatedAssetMetadata(request, output, response, storedObject.storageKey()),
                storageService.provider());
        assetRepository.saveAndFlush(asset);
        asset.completeUpload(
                storedObject.storedFileName(),
                response.outputFormat().isImage() ? AssetFileType.IMAGE : AssetFileType.VIDEO,
                response.mimeType(),
                response.outputFormat().extension(),
                response.fileSize(),
                storageService.provider(),
                storedObject.bucket(),
                storedObject.storageKey(),
                storedObject.publicUrl(),
                storedObject.previewUrl(),
                storedObject.thumbnailUrl(),
                response.width(),
                response.height(),
                response.duration());
        asset.attachStorageFile(storageFileService.registerGeneratedOutput(
                request.getWorkspaceId(),
                request.getProjectId(),
                storageService.provider(),
                storedObject.bucket(),
                storedObject.storageKey(),
                storedObject.publicUrl(),
                response.mimeType(),
                response.outputFormat().extension(),
                response.fileSize(),
                response.width(),
                response.height(),
                response.duration(),
                response.content()).getId());
        assetRepository.save(asset);

        StorageService.SignedAssetUrl previewUrl = storageService.generatePreviewUrl(asset);
        StorageService.SignedAssetUrl downloadUrl = storageService.generateDownloadUrl(asset);
        output.complete(
                asset.getId(),
                asset.getStorageFileId(),
                response.width(),
                response.height(),
                response.duration(),
                response.fileSize(),
                previewUrl.url(),
                downloadUrl.url(),
                response.caption(),
                response.headline(),
                response.ctaText(),
                outputMetadata(response, storedObject.storageKey()));

        request.markCompleted(response.providerName(), response.model());
        job.markCompleted(response.providerJobId());
        outputRepository.save(output);
        requestRepository.save(request);
        jobRepository.save(job);
        creditService.commitCreditUsage(request);
        assetActivityLogger.logAssetUploaded(
                asset.getWorkspaceId(),
                asset.getId(),
                request.getUserId(),
                asset.getAssetCategory(),
                asset.getStorageKey());
        activityLogger.logJobCompleted(
                request.getWorkspaceId(),
                request.getId(),
                job.getId(),
                output.getId(),
                asset.getId(),
                response.providerName(),
                response.model());
    }

    @Transactional
    public void fail(UUID jobId, String providerName, Throwable exception) {
        GenerationJobEntity job = requireJob(jobId);
        CreativeGenerationRequestEntity request = requireRequest(job.getWorkspaceId(), job.getRequestId());
        String reason = safeReason(exception);
        activityLogger.logProviderError(request.getWorkspaceId(), request.getId(), job.getId(), providerName, reason);
        if (request.getStatus() == CreativeGenerationStatus.CANCELLED) {
            job.markCancelled("Request was cancelled");
            jobRepository.save(job);
            creditService.releaseReservedCredits(request);
            return;
        }
        if (job.canRetry()) {
            job.markQueuedForRetry(reason);
            request.markQueuedForRetry(reason);
            jobRepository.save(job);
            requestRepository.save(request);
            activityLogger.logJobFailed(request.getWorkspaceId(), request.getId(), job.getId(), CreativeGenerationStatus.QUEUED, job.getAttemptCount(), reason);
            jobEventPublisher.publish(new GenerationJobQueuedEvent(job.getId(), job.getWorkspaceId(), request.getId(), job.getQueueName()));
            return;
        }
        job.markFailed(reason);
        request.markFailed(reason);
        jobRepository.save(job);
        requestRepository.save(request);
        creditService.releaseReservedCredits(request);
        activityLogger.logJobFailed(request.getWorkspaceId(), request.getId(), job.getId(), CreativeGenerationStatus.FAILED, job.getAttemptCount(), reason);
    }

    private GenerationWorkItem toWorkItem(GenerationJobEntity job, CreativeGenerationRequestEntity request) {
        Map<String, Object> generationConfig = jsonCodec.readMap(request.getGenerationConfig());
        return new GenerationWorkItem(
                job.getId(),
                request.getWorkspaceId(),
                request.getId(),
                request.getUserId(),
                request.getCreativeType(),
                request.getPlatform(),
                request.getCampaignObjective(),
                request.getOutputFormat(),
                request.getLanguage(),
                request.getSourcePrompt(),
                request.getEnhancedPrompt(),
                request.getBrandContextSnapshot(),
                request.getAssetContextSnapshot(),
                request.getGenerationConfig(),
                generationConfig,
                integerValue(generationConfig.get("width")),
                integerValue(generationConfig.get("height")),
                longValue(generationConfig.get("duration")),
                job.getAttemptCount());
    }

    private GenerationJobEntity requireJob(UUID jobId) {
        return jobRepository.findByIdAndDeletedFalse(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GENERATION_JOB_NOT_FOUND));
    }

    private CreativeGenerationRequestEntity requireRequest(UUID workspaceId, UUID requestId) {
        return requestRepository.findByIdAndWorkspaceIdAndDeletedFalse(requestId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GENERATION_REQUEST_NOT_FOUND));
    }

    private Set<String> generatedTags(CreativeGenerationRequestEntity request) {
        return Set.of(
                "generated",
                request.getCreativeType().name().toLowerCase(),
                request.getPlatform().name().toLowerCase());
    }

    private String generatedAssetMetadata(
            CreativeGenerationRequestEntity request,
            CreativeOutputEntity output,
            AiGenerationResponse response,
            String storageKey
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "creative_generation");
        metadata.put("generationRequestId", request.getId());
        metadata.put("creativeOutputId", output.getId());
        metadata.put("creativeType", request.getCreativeType().name());
        metadata.put("platform", request.getPlatform().name());
        metadata.put("provider", response.providerName());
        metadata.put("model", response.model());
        metadata.put("providerJobId", response.providerJobId());
        metadata.put("storageKey", storageKey);
        return jsonCodec.write(metadata, "Generated asset metadata could not be serialized");
    }

    private String outputMetadata(AiGenerationResponse response, String storageKey) {
        Map<String, Object> metadata = new LinkedHashMap<>(response.metadata());
        metadata.put("provider", response.providerName());
        metadata.put("model", response.model());
        metadata.put("providerJobId", response.providerJobId());
        metadata.put("storageKey", storageKey);
        return jsonCodec.write(metadata, "Creative output metadata could not be serialized");
    }

    private String safeReason(Throwable exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        String normalized = message.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 1000 ? normalized : normalized.substring(0, 1000);
    }

    private Integer requestDimensionWidth(CreativeGenerationRequestEntity request) {
        return integerValue(jsonCodec.readMap(request.getGenerationConfig()).get("width"));
    }

    private Integer requestDimensionHeight(CreativeGenerationRequestEntity request) {
        return integerValue(jsonCodec.readMap(request.getGenerationConfig()).get("height"));
    }

    private Integer integerValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        return null;
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        return null;
    }
}
