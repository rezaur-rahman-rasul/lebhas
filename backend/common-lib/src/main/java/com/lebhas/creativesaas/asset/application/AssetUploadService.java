package com.lebhas.creativesaas.asset.application;

import com.lebhas.creativesaas.asset.application.dto.AssetView;
import com.lebhas.creativesaas.asset.application.dto.UploadAssetCommand;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.domain.UploadSessionEntity;
import com.lebhas.creativesaas.asset.infrastructure.persistence.AssetRepository;
import com.lebhas.creativesaas.asset.storage.StoragePathBuilder;
import com.lebhas.creativesaas.asset.storage.StorageService;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.redis.RedisKeyBuilder;
import com.lebhas.creativesaas.redis.RedisLockService;
import com.lebhas.creativesaas.storage.application.StorageFileService;
import com.lebhas.creativesaas.storage.application.StorageMetadataExtractor;
import com.lebhas.creativesaas.storage.domain.StorageFileEntity;
import com.lebhas.creativesaas.storage.domain.StorageUsageEntity;
import com.lebhas.creativesaas.storage.event.AssetUploadCompletedEvent;
import com.lebhas.creativesaas.storage.producer.StoragePlanEventProducer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class AssetUploadService {

    private final AssetValidationService assetValidationService;
    private final AssetRepository assetRepository;
    private final AssetMetadataSerializer assetMetadataSerializer;
    private final AssetMapper assetMapper;
    private final AssetActivityLogger assetActivityLogger;
    private final StorageFileService storageFileService;
    private final StorageService storageService;
    private final StoragePathBuilder storagePathBuilder;
    private final RedisLockService redisLockService;
    private final RedisKeyBuilder redisKeyBuilder;
    private final AssetCacheService assetCacheService;
    private final UploadSessionService uploadSessionService;
    private final UploadDeduplicationService uploadDeduplicationService;
    private final PreviewStateService previewStateService;
    private final AssetEventPublisher assetEventPublisher;
    private final AssetStorageUsageService assetStorageUsageService;
    private final PlanAwareAssetQuotaValidationService planAwareAssetQuotaValidationService;
    private final StoragePlanEventProducer storagePlanEventProducer;

    public AssetUploadService(
            AssetValidationService assetValidationService,
            AssetRepository assetRepository,
            AssetMetadataSerializer assetMetadataSerializer,
            AssetMapper assetMapper,
            AssetActivityLogger assetActivityLogger,
            StorageFileService storageFileService,
            StorageService storageService,
            StoragePathBuilder storagePathBuilder,
            RedisLockService redisLockService,
            RedisKeyBuilder redisKeyBuilder,
            AssetCacheService assetCacheService,
            UploadSessionService uploadSessionService,
            UploadDeduplicationService uploadDeduplicationService,
            PreviewStateService previewStateService,
            AssetEventPublisher assetEventPublisher,
            AssetStorageUsageService assetStorageUsageService,
            PlanAwareAssetQuotaValidationService planAwareAssetQuotaValidationService,
            StoragePlanEventProducer storagePlanEventProducer
    ) {
        this.assetValidationService = assetValidationService;
        this.assetRepository = assetRepository;
        this.assetMetadataSerializer = assetMetadataSerializer;
        this.assetMapper = assetMapper;
        this.assetActivityLogger = assetActivityLogger;
        this.storageFileService = storageFileService;
        this.storageService = storageService;
        this.storagePathBuilder = storagePathBuilder;
        this.redisLockService = redisLockService;
        this.redisKeyBuilder = redisKeyBuilder;
        this.assetCacheService = assetCacheService;
        this.uploadSessionService = uploadSessionService;
        this.uploadDeduplicationService = uploadDeduplicationService;
        this.previewStateService = previewStateService;
        this.assetEventPublisher = assetEventPublisher;
        this.assetStorageUsageService = assetStorageUsageService;
        this.planAwareAssetQuotaValidationService = planAwareAssetQuotaValidationService;
        this.storagePlanEventProducer = storagePlanEventProducer;
    }

    @Transactional
    public AssetView uploadAsset(UploadAssetCommand command) {
        AssetValidationService.UploadValidationContext validation = assetValidationService.validateUpload(command);
        StorageMetadataExtractor.ExtractedMetadata metadata = validation.metadata();
        RedisLockService.RedisLockToken uploadLock = uploadDeduplicationService.acquire(metadata.sha256());
        UploadSessionEntity uploadSession = null;
        AssetEntity asset = null;
        PlanAwareAssetQuotaValidationService.UploadQuotaGuard uploadQuotaGuard = null;
        try {
            uploadSession = uploadSessionService.createSession(
                    command.workspaceId(),
                    validation.campaignContext().brandId(),
                    validation.campaignContext().productServiceId(),
                    command.projectId(),
                    validation.access().currentUser().userId(),
                    metadata.originalFileName(),
                    metadata.mimeType(),
                    metadata.fileSize(),
                    metadata.sha256(),
                    1);

            asset = createUploadingAsset(command, validation, uploadSession.getId());
            uploadSession = uploadSessionService.attachAsset(uploadSession, asset.getId());
            previewStateService.markPending(asset.getId());
            assetActivityLogger.logUploadStarted(
                    command.workspaceId(),
                    asset.getId(),
                    validation.access().currentUser().userId(),
                    uploadSession.getId(),
                    validation.assetCategory(),
                    validation.assetType());

            uploadQuotaGuard = planAwareAssetQuotaValidationService.acquireUploadQuotaGuard(
                    command.workspaceId(),
                    asset.getId(),
                    command.projectId(),
                    validation.assetType(),
                    metadata.fileSize());
            String objectKey = command.projectId() == null
                    ? storagePathBuilder.buildWorkspaceAssetPath(command.workspaceId(), asset.getId(), metadata.sanitizedFileName())
                    : storagePathBuilder.buildAssetPath(
                            command.workspaceId(),
                            command.projectId(),
                            asset.getId(),
                            metadata.sanitizedFileName());
            RedisLockService.RedisLockToken storageLock = acquireStorageLock(objectKey);
            StorageService.StoredObject storedObject;
            try {
                storedObject = storageService.store(new StorageService.StorageUploadRequest(
                        command.workspaceId(),
                        command.projectId(),
                        asset.getId(),
                        metadata.sanitizedFileName(),
                        metadata.mimeType(),
                        command.file()));
            } finally {
                redisLockService.release(storageLock);
            }

            StorageFileEntity storageFile = storageFileService.registerRawUpload(
                    command.workspaceId(),
                    command.projectId(),
                    storageService.provider(),
                    storedObject.bucket(),
                    storedObject.storageKey(),
                    storedObject.publicUrl(),
                    metadata);

            asset.attachStorageFile(storageFile.getId());
            asset.completeUpload(
                    metadata.sanitizedFileName(),
                    metadata.fileType(),
                    metadata.mimeType(),
                    metadata.fileExtension(),
                    metadata.fileSize(),
                    storageService.provider(),
                    storedObject.bucket(),
                    storedObject.storageKey(),
                    metadata.sha256(),
                    storedObject.publicUrl(),
                    storedObject.previewUrl(),
                    storedObject.thumbnailUrl(),
                    metadata.width(),
                    metadata.height(),
                    metadata.duration());
            asset.markPreviewReady();
            asset = assetRepository.save(asset);
            uploadSession = uploadSessionService.markSingleChunkUploaded(uploadSession);
            uploadSession = uploadSessionService.markCompleted(uploadSession);
            previewStateService.markReady(asset.getId(), true);
            assetActivityLogger.logPreviewState(command.workspaceId(), asset.getId(), "READY");
            StorageUsageEntity usage = assetStorageUsageService.recordUpload(
                    asset,
                    metadata.fileSize(),
                    uploadQuotaGuard == null ? null : uploadQuotaGuard.policy().maxStorageBytes(),
                    "ASSET_UPLOAD_COMPLETED");
            AssetView uploadedView = persistAndCache(asset, validation.access().currentUser().userId());
            uploadDeduplicationService.rememberDuplicate(metadata.sha256(), asset.getId(), command.workspaceId(), command.projectId());
            return uploadedView;
        } catch (RuntimeException exception) {
            if (asset != null) {
                asset.markUploadFailed();
                assetRepository.save(asset);
                previewStateService.markFailed(asset.getId(), safeReason(exception));
                assetActivityLogger.logUploadFailed(
                        command.workspaceId(),
                        asset.getId(),
                        validation.access().currentUser().userId(),
                        safeReason(exception));
                assetEventPublisher.publish(
                        KafkaTopicConstants.ASSET_UPLOAD_FAILED,
                        command.workspaceId(),
                        asset.getId(),
                        Map.of(
                                "workspaceId", command.workspaceId().toString(),
                                "assetId", asset.getId().toString(),
                                "projectId", projectIdValue(command.projectId()),
                                "reason", safeReason(exception)));
            }
            if (uploadSession != null) {
                uploadSessionService.markFailed(uploadSession, safeReason(exception));
            }
            throw exception;
        } finally {
            planAwareAssetQuotaValidationService.releaseUploadQuotaGuard(uploadQuotaGuard);
            uploadDeduplicationService.release(uploadLock);
        }
    }

    private AssetEntity createUploadingAsset(
            UploadAssetCommand command,
            AssetValidationService.UploadValidationContext validation,
            UUID uploadSessionId
    ) {
        AssetEntity asset = AssetEntity.createUploading(
                command.workspaceId(),
                validation.campaignContext().brandId(),
                validation.campaignContext().productServiceId(),
                command.projectId(),
                validation.access().currentUser().userId(),
                null,
                validation.assetType(),
                validation.assetCategory(),
                validation.metadata().originalFileName(),
                command.displayName(),
                command.description(),
                validation.tags(),
                uploadSessionId,
                assetMetadataSerializer.serialize(command.metadata()),
                storageService.provider());
        asset.recordChecksum(validation.metadata().sha256());
        return assetRepository.saveAndFlush(asset);
    }

    private AssetView completeWithDuplicateStorage(
            AssetEntity asset,
            AssetValidationService.UploadValidationContext validation,
            UploadSessionEntity uploadSession,
            StorageFileEntity duplicateStorage
    ) {
        asset.attachStorageFile(duplicateStorage.getId());
        asset.completeUpload(
                validation.metadata().sanitizedFileName(),
                validation.metadata().fileType(),
                duplicateStorage.getMimeType(),
                duplicateStorage.getFileExtension(),
                duplicateStorage.getFileSize(),
                duplicateStorage.getProvider(),
                duplicateStorage.getBucket(),
                duplicateStorage.getObjectKey(),
                validation.metadata().sha256(),
                duplicateStorage.getCdnUrl(),
                duplicateStorage.getCdnUrl(),
                null,
                duplicateStorage.getWidth(),
                duplicateStorage.getHeight(),
                duplicateStorage.getDuration());
        asset.markPreviewReady();
        asset = assetRepository.save(asset);
        uploadSessionService.markSingleChunkUploaded(uploadSession);
        uploadSessionService.markCompleted(uploadSession);
        previewStateService.markReady(asset.getId(), true);
        assetActivityLogger.logDuplicateUploadAttempt(
                asset.getWorkspaceId(),
                validation.access().currentUser().userId(),
                validation.metadata().sha256(),
                asset.getId());
        PlanAwareAssetQuotaValidationService.PlanUploadPolicy policy =
                planAwareAssetQuotaValidationService.resolveUploadPolicy(asset.getWorkspaceId(), validation.assetType());
        StorageUsageEntity usage = assetStorageUsageService.recordUpload(
                asset,
                0L,
                policy.maxStorageBytes(),
                "ASSET_UPLOAD_COMPLETED");
        AssetView duplicateView = persistAndCache(asset, validation.access().currentUser().userId());
        publishUploadCompleted(asset, asset.getProjectId(), uploadSession.getId(), validation.assetType(), usage, policy);
        return duplicateView;
    }

    private AssetView findExistingDuplicate(UUID workspaceId, UUID projectId, String sha256) {
        UUID rememberedAssetId = uploadDeduplicationService.findRememberedDuplicate(sha256).orElse(null);
        if (rememberedAssetId != null) {
            AssetEntity remembered = assetRepository.findByIdAndWorkspaceIdAndDeletedFalse(rememberedAssetId, workspaceId).orElse(null);
            if (remembered != null && Objects.equals(remembered.getProjectId(), projectId) && remembered.isReady()) {
                return assetStorageObjectExists(remembered) ? assetMapper.toAssetView(remembered) : null;
            }
        }
        StorageFileEntity duplicateStorage = storageFileService.findDuplicateByHash(workspaceId, sha256).orElse(null);
        if (duplicateStorage == null) {
            return null;
        }
        if (!storageObjectExists(duplicateStorage)) {
            return null;
        }
        AssetEntity duplicateAsset = assetRepository.findFirstByWorkspaceIdAndProjectIdAndStorageFileIdAndDeletedFalse(
                        workspaceId,
                        projectId,
                        duplicateStorage.getId())
                .orElse(null);
        if (duplicateAsset == null || !duplicateAsset.isReady()) {
            return null;
        }
        return assetMapper.toAssetView(duplicateAsset);
    }

    private boolean storageObjectExists(StorageFileEntity storageFile) {
        if (storageFile == null
                || storageFile.getObjectKey() == null
                || storageFile.getObjectKey().isBlank()) {
            return false;
        }
        try {
            storageService.getMetadata(storageFile.getBucket(), storageFile.getObjectKey());
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean assetStorageObjectExists(AssetEntity asset) {
        if (asset == null
                || asset.getStorageKey() == null
                || asset.getStorageKey().isBlank()) {
            return false;
        }
        try {
            storageService.getMetadata(asset);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private AssetView persistAndCache(AssetEntity asset, UUID actorUserId) {
        AssetView view = assetMapper.toAssetView(assetRepository.save(asset));
        assetCacheService.invalidate(asset.getWorkspaceId(), asset.getProjectId(), asset.getId(), actorUserId);
        assetCacheService.cacheAsset(view);
        return view;
    }

    private void logUploadStarted(
            UploadAssetCommand command,
            AssetValidationService.UploadValidationContext validation,
            UUID uploadSessionId,
            UUID assetId
    ) {
        assetActivityLogger.logUploadStarted(
                command.workspaceId(),
                assetId,
                validation.access().currentUser().userId(),
                uploadSessionId,
                validation.assetCategory(),
                validation.assetType());
        assetEventPublisher.publish(
                KafkaTopicConstants.ASSET_UPLOAD_STARTED,
                command.workspaceId(),
                assetId,
                Map.of(
                        "workspaceId", command.workspaceId().toString(),
                        "assetId", assetId.toString(),
                        "projectId", projectIdValue(command.projectId()),
                        "uploadSessionId", uploadSessionId.toString()));
    }

    private void publishUploadCompleted(
            AssetEntity asset,
            UUID projectId,
            UUID uploadSessionId,
            com.lebhas.creativesaas.asset.domain.AssetType assetType,
            StorageUsageEntity usage,
            PlanAwareAssetQuotaValidationService.PlanUploadPolicy policy
    ) {
        assetActivityLogger.logUploadCompleted(asset.getWorkspaceId(), asset.getId(), asset.getUploadedBy(), asset.getStorageKey());
        try {
            storagePlanEventProducer.publishAssetUploadCompleted(new AssetUploadCompletedEvent(
                    null,
                    null,
                    asset.getWorkspaceId(),
                    asset.getId(),
                    projectId,
                    uploadSessionId,
                    policy == null || policy.pricingPlan() == null ? null : policy.pricingPlan().id(),
                    policy == null || policy.subscription() == null ? null : policy.subscription().id(),
                    policy == null || policy.pricingPlan() == null ? null : policy.pricingPlan().code(),
                    policy == null || policy.subscription() == null || policy.subscription().status() == null ? null : policy.subscription().status().name(),
                    assetType == null ? null : assetType.name(),
                    Math.max(asset.getFileSize(), 0L),
                    usage == null ? 0L : usage.getTotalUsedBytes(),
                    policy == null ? null : policy.maxStorageBytes(),
                    "ASSET_UPLOAD_COMPLETED"));
        } catch (RuntimeException exception) {
            assetActivityLogger.logKafkaFailure(KafkaTopicConstants.ASSET_UPLOAD_COMPLETED, asset.getWorkspaceId(), asset.getId(), exception.getMessage());
        }
        assetEventPublisher.publishUploaded(asset, uploadSessionId);
    }

    private void publishPreviewStarted(AssetEntity asset) {
        assetEventPublisher.publish(
                KafkaTopicConstants.ASSET_PREVIEW_STARTED,
                asset.getWorkspaceId(),
                asset.getId(),
                Map.of(
                        "workspaceId", asset.getWorkspaceId().toString(),
                        "assetId", asset.getId().toString(),
                        "projectId", projectIdValue(asset.getProjectId())));
    }

    private void publishPreviewCompleted(AssetEntity asset) {
        assetEventPublisher.publish(
                KafkaTopicConstants.ASSET_PREVIEW_COMPLETED,
                asset.getWorkspaceId(),
                asset.getId(),
                Map.of(
                        "workspaceId", asset.getWorkspaceId().toString(),
                        "assetId", asset.getId().toString(),
                        "projectId", projectIdValue(asset.getProjectId())));
    }

    private void publishProcessRequested(AssetEntity asset) {
        assetEventPublisher.publishProcess(asset);
    }

    private RedisLockService.RedisLockToken acquireStorageLock(String objectKey) {
        return redisLockService.acquire(redisKeyBuilder.lockStorage(objectKey), Duration.ofSeconds(30))
                .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Storage mutation is already in progress"));
    }

    private String safeReason(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return "Unknown failure";
        }
        String normalized = throwable.getMessage().replaceAll("\\s+", " ").trim();
        return normalized.length() <= 1000 ? normalized : normalized.substring(0, 1000);
    }

    private String projectIdValue(UUID projectId) {
        return projectId == null ? "" : projectId.toString();
    }
}
