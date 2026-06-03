package com.lebhas.creativesaas.asset.application;

import com.lebhas.creativesaas.asset.application.dto.AssetUploadUrlView;
import com.lebhas.creativesaas.asset.application.dto.AssetView;
import com.lebhas.creativesaas.asset.application.dto.ConfirmAssetUploadCommand;
import com.lebhas.creativesaas.asset.application.dto.CreateAssetUploadUrlCommand;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.domain.AssetFileType;
import com.lebhas.creativesaas.asset.domain.AssetStatus;
import com.lebhas.creativesaas.asset.domain.AssetType;
import com.lebhas.creativesaas.asset.domain.UploadSessionEntity;
import com.lebhas.creativesaas.asset.infrastructure.persistence.AssetRepository;
import com.lebhas.creativesaas.asset.storage.R2StorageProperties;
import com.lebhas.creativesaas.asset.storage.StoragePathBuilder;
import com.lebhas.creativesaas.asset.storage.StorageProperties;
import com.lebhas.creativesaas.asset.storage.StorageService;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.storage.application.StorageFileService;
import com.lebhas.creativesaas.storage.domain.StorageFileEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
public class SignedAssetUploadService {

    private static final Logger log = LoggerFactory.getLogger(SignedAssetUploadService.class);

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final AssetFileValidationService assetFileValidationService;
    private final AssetValidationService assetValidationService;
    private final AssetRepository assetRepository;
    private final UploadSessionService uploadSessionService;
    private final AssetFolderService assetFolderService;
    private final StorageService storageService;
    private final StorageProperties storageProperties;
    private final R2StorageProperties r2StorageProperties;
    private final StoragePathBuilder storagePathBuilder;
    private final AssetMetadataSerializer assetMetadataSerializer;
    private final AssetMapper assetMapper;
    private final AssetCacheService assetCacheService;
    private final AssetActivityLogger assetActivityLogger;
    private final AssetEventPublisher assetEventPublisher;
    private final AssetStorageUsageService assetStorageUsageService;
    private final PlanAwareAssetQuotaValidationService planAwareAssetQuotaValidationService;
    private final StorageFileService storageFileService;
    private final AssetServiceProperties assetServiceProperties;
    private final Duration uploadUrlTtl;

    public SignedAssetUploadService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            AssetFileValidationService assetFileValidationService,
            AssetValidationService assetValidationService,
            AssetRepository assetRepository,
            UploadSessionService uploadSessionService,
            AssetFolderService assetFolderService,
            StorageService storageService,
            StorageProperties storageProperties,
            R2StorageProperties r2StorageProperties,
            StoragePathBuilder storagePathBuilder,
            AssetMetadataSerializer assetMetadataSerializer,
            AssetMapper assetMapper,
            AssetCacheService assetCacheService,
            AssetActivityLogger assetActivityLogger,
            AssetEventPublisher assetEventPublisher,
            AssetStorageUsageService assetStorageUsageService,
            PlanAwareAssetQuotaValidationService planAwareAssetQuotaValidationService,
            StorageFileService storageFileService,
            AssetServiceProperties assetServiceProperties,
            @Value("${platform.asset.upload-url-ttl:PT15M}") Duration uploadUrlTtl
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.assetFileValidationService = assetFileValidationService;
        this.assetValidationService = assetValidationService;
        this.assetRepository = assetRepository;
        this.uploadSessionService = uploadSessionService;
        this.assetFolderService = assetFolderService;
        this.storageService = storageService;
        this.storageProperties = storageProperties;
        this.r2StorageProperties = r2StorageProperties;
        this.storagePathBuilder = storagePathBuilder;
        this.assetMetadataSerializer = assetMetadataSerializer;
        this.assetMapper = assetMapper;
        this.assetCacheService = assetCacheService;
        this.assetActivityLogger = assetActivityLogger;
        this.assetEventPublisher = assetEventPublisher;
        this.assetStorageUsageService = assetStorageUsageService;
        this.planAwareAssetQuotaValidationService = planAwareAssetQuotaValidationService;
        this.storageFileService = storageFileService;
        this.assetServiceProperties = assetServiceProperties;
        this.uploadUrlTtl = uploadUrlTtl == null || uploadUrlTtl.isZero() || uploadUrlTtl.isNegative()
                ? Duration.ofMinutes(15)
                : uploadUrlTtl;
    }

    @Transactional
    public AssetUploadUrlView createUploadUrl(CreateAssetUploadUrlCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access =
                workspaceAuthorizationService.requirePermission(command.workspaceId(), Permission.ASSET_UPLOAD);
        AssetFileValidationService.ValidatedAssetFile metadata = assetFileValidationService.validateMetadata(
                command.originalFileName(),
                command.contentType(),
                command.sizeBytes(),
                command.assetCategory());
        AssetValidationService.CampaignContext campaignContext = command.projectId() == null
                ? new AssetValidationService.CampaignContext(null, null, null)
                : assetValidationService.validateProjectContext(command.workspaceId(), command.projectId());
        UUID folderId = validateFolderId(command.workspaceId(), command.folderId());
        assetValidationService.validateWorkspaceQuota(command.workspaceId(), metadata.size());
        AssetType resolvedType = assetValidationService.resolveUploadAssetType(command.assetType(), command.assetCategory(), metadata.fileType());
        planAwareAssetQuotaValidationService.resolveUploadPolicy(command.workspaceId(), resolvedType);
        String bucket = bucket();
        UploadSessionEntity session = uploadSessionService.createSession(
                command.workspaceId(),
                campaignContext.brandId(),
                campaignContext.productServiceId(),
                command.projectId(),
                access.currentUser().userId(),
                metadata.originalFileName(),
                metadata.mimeType(),
                metadata.size(),
                normalizeChecksum(command.checksum()),
                1);
        AssetEntity asset = AssetEntity.createSignedUploadPending(
                command.workspaceId(),
                campaignContext.brandId(),
                campaignContext.productServiceId(),
                command.projectId(),
                access.currentUser().userId(),
                folderId,
                resolvedType,
                assetValidationService.resolveAssetCategory(command.assetCategory(), resolvedType, metadata.fileType()),
                metadata.originalFileName(),
                metadata.sanitizedFileName(),
                command.displayName(),
                command.description(),
                assetValidationService.normalizeTags(command.tags()),
                session.getId(),
                assetMetadataSerializer.serialize(command.metadata()),
                storageService.provider(),
                bucket,
                null,
                metadata.mimeType(),
                metadata.extension(),
                metadata.size(),
                normalizeChecksum(command.checksum()),
                "USER_UPLOAD");
        asset = persistNewSignedUploadAsset(asset);
        String objectKey = command.projectId() == null
                ? storagePathBuilder.buildWorkspaceAssetPath(command.workspaceId(), asset.getId(), metadata.sanitizedFileName())
                : storagePathBuilder.buildAssetPath(command.workspaceId(), command.projectId(), asset.getId(), metadata.sanitizedFileName());
        asset.configureSignedUploadStorage(bucket, objectKey);
        asset = assetRepository.saveAndFlush(asset);
        uploadSessionService.attachAsset(session, asset.getId());
        StorageService.SignedAssetUrl signedUrl = generateSignedUploadUrl(
                command.workspaceId(),
                bucket,
                objectKey,
                metadata.mimeType(),
                metadata.size(),
                uploadUrlTtl);
        assetActivityLogger.logUploadStarted(
                command.workspaceId(),
                asset.getId(),
                access.currentUser().userId(),
                session.getId(),
                asset.getAssetCategory(),
                asset.getAssetType());
        assetEventPublisher.publish(
                KafkaTopicConstants.ASSET_UPLOAD_URL_CREATED,
                command.workspaceId(),
                asset.getId(),
                Map.of(
                        "workspaceId", command.workspaceId().toString(),
                        "assetId", asset.getId().toString(),
                        "uploadSessionId", session.getId().toString(),
                        "projectId", command.projectId() == null ? "" : command.projectId().toString()));
        return new AssetUploadUrlView(
                asset.getId(),
                session.getId(),
                signedUrl.url(),
                "PUT",
                Map.of(),
                signedUrl.expiresAt(),
                maxFileSize(command.assetCategory()));
    }

    private AssetEntity persistNewSignedUploadAsset(AssetEntity asset) {
        return assetRepository.saveAndFlush(asset);
    }

    private StorageService.SignedAssetUrl generateSignedUploadUrl(
            UUID workspaceId,
            String bucket,
            String objectKey,
            String mimeType,
            long sizeBytes,
            Duration ttl
    ) {
        try {
            return storageService.generateUploadUrl(bucket, objectKey, mimeType, sizeBytes, ttl);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Failed to generate signed asset upload URL for workspace {}", workspaceId, exception);
            throw new BusinessException(
                    ErrorCode.ASSET_STORAGE_FAILURE,
                    "Asset upload URL could not be generated. Verify storage configuration and retry.");
        }
    }

    @Transactional
    public AssetView confirmUpload(ConfirmAssetUploadCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access =
                workspaceAuthorizationService.requirePermission(command.workspaceId(), Permission.ASSET_UPLOAD);
        AssetEntity asset = assetValidationService.requireAsset(command.workspaceId(), command.assetId());
        if (asset.getStatus() != AssetStatus.UPLOAD_PENDING && asset.getStatus() != AssetStatus.UPLOADING) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Asset upload has already been confirmed");
        }
        if (command.uploadReferenceId() != null && !command.uploadReferenceId().equals(asset.getUploadSessionId())) {
            throw new BusinessException(ErrorCode.UPLOAD_SESSION_NOT_FOUND);
        }
        StorageService.StoredObjectMetadata metadata = storageService.getMetadata(asset);
        if (metadata.contentLength() != asset.getFileSize()) {
            throw new BusinessException(ErrorCode.ASSET_METADATA_INVALID, "Uploaded file size does not match the signed upload request");
        }
        StorageFileEntity storageFile = storageFileService.registerConfirmedUpload(
                asset.getWorkspaceId(),
                asset.getProjectId(),
                asset.getStorageProvider(),
                asset.getStorageBucket(),
                asset.getStorageKey(),
                null,
                asset.getMimeType(),
                asset.getFileExtension(),
                asset.getFileSize(),
                StringUtils.hasText(command.checksum()) ? normalizeChecksum(command.checksum()) : asset.getChecksum());
        asset.confirmSignedUpload(storageFile.getId(), null, null, null);
        asset = assetRepository.save(asset);
        UploadSessionEntity session = uploadSessionService.requireSession(asset.getWorkspaceId(), asset.getUploadSessionId());
        uploadSessionService.markSingleChunkUploaded(session);
        uploadSessionService.markCompleted(session);
        assetStorageUsageService.recordUpload(asset, asset.getFileSize(), null, "ASSET_UPLOAD_CONFIRMED");
        assetCacheService.invalidate(asset.getWorkspaceId(), asset.getProjectId(), asset.getId(), access.currentUser().userId());
        AssetView view = assetMapper.toAssetView(asset);
        assetCacheService.cacheAsset(view);
        assetActivityLogger.logUploadCompleted(asset.getWorkspaceId(), asset.getId(), access.currentUser().userId(), asset.getStorageKey());
        assetEventPublisher.publish(
                KafkaTopicConstants.ASSET_UPLOAD_CONFIRMED,
                asset.getWorkspaceId(),
                asset.getId(),
                Map.of(
                        "workspaceId", asset.getWorkspaceId().toString(),
                        "assetId", asset.getId().toString(),
                        "projectId", asset.getProjectId() == null ? "" : asset.getProjectId().toString()));
        return view;
    }

    private String bucket() {
        if (StringUtils.hasText(r2StorageProperties.getBucket())) {
            return r2StorageProperties.getBucket().trim();
        }
        return storageProperties.getBucket();
    }

    private String normalizeChecksum(String checksum) {
        if (!StringUtils.hasText(checksum)) {
            return null;
        }
        return checksum.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private UUID validateFolderId(UUID workspaceId, UUID folderId) {
        if (folderId == null) {
            return null;
        }
        return assetFolderService.requireFolder(workspaceId, folderId).getId();
    }

    private long maxFileSize(com.lebhas.creativesaas.asset.domain.AssetCategory category) {
        return switch (category == null ? com.lebhas.creativesaas.asset.domain.AssetCategory.OTHER : category) {
            case BRAND_LOGO -> assetServiceProperties.getMaxLogoSizeBytes();
            case PRODUCT_VIDEO, EXPORT_VIDEO, REFERENCE_VIDEO -> assetServiceProperties.getMaxVideoSizeBytes();
            case PRODUCT_IMAGE, PACKAGING_IMAGE, EXPORT_IMAGE, REFERENCE_IMAGE -> assetServiceProperties.getMaxImageSizeBytes();
            default -> assetServiceProperties.getMaxUploadSizeBytes();
        };
    }
}
