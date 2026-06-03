package com.lebhas.creativesaas.download.application;

import com.lebhas.creativesaas.asset.application.AssetService;
import com.lebhas.creativesaas.asset.application.dto.AssetUrlView;
import com.lebhas.creativesaas.asset.cache.AssetCacheTtlStrategy;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.domain.StorageProvider;
import com.lebhas.creativesaas.asset.storage.SignedUrlRequest;
import com.lebhas.creativesaas.asset.storage.SignedUrlResponse;
import com.lebhas.creativesaas.asset.storage.StorageProperties;
import com.lebhas.creativesaas.asset.storage.StorageService;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.download.application.dto.DownloadRequestContext;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.generatedversion.domain.ApprovalStatus;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.pricing.application.WorkspacePlanContextService;
import com.lebhas.creativesaas.pricing.application.dto.PlanFeaturePolicyView;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.asset.application.AssetEventPublisher;
import com.lebhas.creativesaas.redis.RedisSignedUrlCache;
import com.lebhas.creativesaas.sharing.application.ShareLinkService;
import com.lebhas.creativesaas.storage.application.StorageFileService;
import com.lebhas.creativesaas.storage.domain.StorageFileEntity;
import com.lebhas.creativesaas.usage.application.ShareUsageAccessService;
import com.lebhas.creativesaas.usage.application.dto.ShareUsageTrackingCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class DownloadService {

    private static final String SHARE_DOWNLOAD = "share-download";

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final AssetService assetService;
    private final DownloadHistoryService downloadHistoryService;
    private final ShareLinkService shareLinkService;
    private final StorageFileService storageFileService;
    private final StorageService storageService;
    private final RedisSignedUrlCache redisSignedUrlCache;
    private final AssetCacheTtlStrategy assetCacheTtlStrategy;
    private final StorageProperties storageProperties;
    private final ShareUsageAccessService shareUsageAccessService;
    private final WorkspacePlanContextService workspacePlanContextService;
    private final AssetEventPublisher eventPublisher;
    private final Clock clock;

    @Autowired
    public DownloadService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            AssetService assetService,
            DownloadHistoryService downloadHistoryService,
            ShareLinkService shareLinkService,
            StorageFileService storageFileService,
            StorageService storageService,
            RedisSignedUrlCache redisSignedUrlCache,
            AssetCacheTtlStrategy assetCacheTtlStrategy,
            StorageProperties storageProperties,
            ShareUsageAccessService shareUsageAccessService,
            WorkspacePlanContextService workspacePlanContextService,
            AssetEventPublisher eventPublisher,
            Clock clock
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.assetService = assetService;
        this.downloadHistoryService = downloadHistoryService;
        this.shareLinkService = shareLinkService;
        this.storageFileService = storageFileService;
        this.storageService = storageService;
        this.redisSignedUrlCache = redisSignedUrlCache;
        this.assetCacheTtlStrategy = assetCacheTtlStrategy;
        this.storageProperties = storageProperties;
        this.shareUsageAccessService = shareUsageAccessService;
        this.workspacePlanContextService = workspacePlanContextService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public DownloadService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            AssetService assetService,
            DownloadHistoryService downloadHistoryService,
            ShareLinkService shareLinkService,
            StorageFileService storageFileService,
            StorageService storageService,
            RedisSignedUrlCache redisSignedUrlCache,
            AssetCacheTtlStrategy assetCacheTtlStrategy,
            StorageProperties storageProperties,
            ShareUsageAccessService shareUsageAccessService,
            Clock clock
    ) {
        this(
                workspaceAuthorizationService,
                assetService,
                downloadHistoryService,
                shareLinkService,
                storageFileService,
                storageService,
                redisSignedUrlCache,
                assetCacheTtlStrategy,
                storageProperties,
                shareUsageAccessService,
                null,
                null,
                clock);
    }

    @Transactional
    public AssetUrlView requestAssetDownload(UUID workspaceId, UUID assetId, DownloadRequestContext requestContext) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(workspaceId, Permission.CREATIVE_DOWNLOAD);
        AssetEntity asset = assetService.requireAsset(workspaceId, assetId);
        AssetUrlView urlView = assetService.generateDownloadUrl(workspaceId, assetId);
        if (asset.getStorageProvider() != StorageProvider.LOCAL) {
            downloadHistoryService.recordAssetDownload(asset, access.currentUser().userId(), requestContext);
        }
        return urlView;
    }

    @Transactional
    public AssetUrlView requestGeneratedVersionDownload(UUID workspaceId, UUID generatedVersionId, DownloadRequestContext requestContext) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(workspaceId, Permission.CREATIVE_DOWNLOAD);
        GeneratedVersionEntity generatedVersion = shareLinkService.requireGeneratedVersionForWorkspace(workspaceId, generatedVersionId);
        PlanFeaturePolicyView policy = requireDownloadPolicy(workspaceId);
        if (!policy.downloadEnabled()) {
            throw new BusinessException(ErrorCode.PLAN_FEATURE_DISABLED, "Downloads are not enabled for the workspace plan");
        }
        if (policy.allowApprovalWorkflow() && generatedVersion.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Generated version must be approved before download");
        }
        StorageFileEntity storageFile = storageFileService.requireStorageFile(workspaceId, generatedVersion.getStorageFileId());
        AssetUrlView urlView = resolveGeneratedVersionDownloadUrl(generatedVersion, storageFile);
        downloadHistoryService.recordGeneratedVersionDownload(
                workspaceId,
                generatedVersionId,
                access.currentUser().userId(),
                requestContext);
        publish(KafkaTopicConstants.GENERATED_VERSION_DOWNLOAD_URL_CREATED, workspaceId, generatedVersionId, Map.of(
                "workspaceId", workspaceId.toString(),
                "generatedVersionId", generatedVersionId.toString(),
                "actorUserId", access.currentUser().userId().toString()));
        publish(KafkaTopicConstants.USAGE_EVENT_RECORDED, workspaceId, generatedVersionId, Map.of(
                "workspaceId", workspaceId.toString(),
                "generatedVersionId", generatedVersionId.toString(),
                "usageType", "DOWNLOAD"));
        return urlView;
    }

    @Transactional
    public AssetUrlView requestPublicShareDownload(String token, String password, DownloadRequestContext requestContext) {
        ShareLinkService.ResolvedShareLink shareLink = shareLinkService.resolvePublicShareLink(token, password);
        GeneratedVersionEntity generatedVersion = shareLink.generatedVersion();
        PlanFeaturePolicyView policy = requireDownloadPolicy(shareLink.workspaceId());
        if (!policy.downloadEnabled() || !policy.allowPublicShareLinks()) {
            throw new BusinessException(ErrorCode.PLAN_FEATURE_DISABLED, "Public share downloads are not enabled for the workspace plan");
        }
        StorageFileEntity storageFile = storageFileService.requireStorageFile(
                shareLink.workspaceId(),
                generatedVersion.getStorageFileId());
        AssetUrlView urlView = resolveGeneratedVersionDownloadUrl(generatedVersion, storageFile);
        shareUsageAccessService.recordPublicShareAccess(new ShareUsageTrackingCommand(
                shareLink.token(),
                null,
                requestContext == null ? null : requestContext.ipAddress(),
                requestContext == null ? null : requestContext.userAgent(),
                null,
                null));
        downloadHistoryService.recordGeneratedVersionDownload(
                shareLink.workspaceId(),
                shareLink.generatedVersionId(),
                null,
                requestContext);
        publish(KafkaTopicConstants.GENERATED_VERSION_SHARE_ACCESSED, shareLink.workspaceId(), shareLink.generatedVersionId(), Map.of(
                "workspaceId", shareLink.workspaceId().toString(),
                "generatedVersionId", shareLink.generatedVersionId().toString()));
        publish(KafkaTopicConstants.USAGE_EVENT_RECORDED, shareLink.workspaceId(), shareLink.generatedVersionId(), Map.of(
                "workspaceId", shareLink.workspaceId().toString(),
                "generatedVersionId", shareLink.generatedVersionId().toString(),
                "usageType", "PUBLIC_SHARE_DOWNLOAD"));
        return urlView;
    }

    private PlanFeaturePolicyView requireDownloadPolicy(UUID workspaceId) {
        if (workspacePlanContextService == null) {
            return new PlanFeaturePolicyView(
                    null, null, null, null, null, null, null, null, null, null, null, null, null,
                    true, true, true, true, true, true, true, true, true, true, true, true, true, null, null);
        }
        PlanFeaturePolicyView featurePolicy = workspacePlanContextService.getWorkspacePlanContext(workspaceId).featurePolicy();
        if (featurePolicy == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "The active workspace plan feature policy is not available");
        }
        return featurePolicy;
    }

    private void publish(String topic, UUID workspaceId, UUID aggregateId, Map<String, Object> attributes) {
        if (eventPublisher != null) {
            eventPublisher.publish(topic, workspaceId, aggregateId, attributes);
        }
    }

    private AssetUrlView resolveGeneratedVersionDownloadUrl(
            GeneratedVersionEntity generatedVersion,
            StorageFileEntity storageFile
    ) {
        RedisSignedUrlCache.SignedUrlSnapshot cached = redisSignedUrlCache.get(storageFile.getId())
                .filter(snapshot -> SHARE_DOWNLOAD.equals(snapshot.type()))
                .filter(snapshot -> snapshot.expiresAt() != null && snapshot.expiresAt().isAfter(Instant.now(clock)))
                .orElse(null);
        if (cached != null) {
            return new AssetUrlView(
                    cached.url(),
                    cached.type(),
                    cached.cdnUrl(),
                    true,
                    cached.generatedAt(),
                    cached.expiresAt());
        }

        com.lebhas.creativesaas.asset.storage.StorageProvider storageProvider = requireStorageProvider();
        SignedUrlResponse signedUrl = storageProvider.generateSignedUrl(new SignedUrlRequest(
                storageFile.getBucket(),
                storageFile.getObjectKey(),
                storageProperties.getSignedUrlTtl(),
                resolveGeneratedVersionFileName(generatedVersion, storageFile),
                true,
                storageFile.getMimeType()));
        Instant generatedAt = Instant.now(clock);
        redisSignedUrlCache.store(
                storageFile.getId(),
                new RedisSignedUrlCache.SignedUrlSnapshot(
                        signedUrl.url(),
                        signedUrl.expiresAt(),
                        SHARE_DOWNLOAD,
                        signedUrl.cdnUrl(),
                        generatedAt),
                assetCacheTtlStrategy.signedUrlTtl(signedUrl.expiresAt()));
        return new AssetUrlView(
                signedUrl.url(),
                SHARE_DOWNLOAD,
                signedUrl.cdnUrl(),
                false,
                generatedAt,
                signedUrl.expiresAt());
    }

    private com.lebhas.creativesaas.asset.storage.StorageProvider requireStorageProvider() {
        if (storageService instanceof com.lebhas.creativesaas.asset.storage.StorageProvider storageProvider) {
            return storageProvider;
        }
        throw new BusinessException(ErrorCode.ASSET_STORAGE_FAILURE, "Configured storage service does not support generic signed delivery");
    }

    private String resolveGeneratedVersionFileName(GeneratedVersionEntity generatedVersion, StorageFileEntity storageFile) {
        String extension = storageFile.getFileExtension();
        if (extension == null || extension.isBlank()) {
            extension = "bin";
        }
        String baseName = generatedVersion.getVersionName();
        if (baseName == null || baseName.isBlank()) {
            baseName = "generated-version-" + generatedVersion.getId();
        }
        String sanitizedBaseName = baseName.trim()
                .replaceAll("[^A-Za-z0-9._-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^[-.]+|[-.]+$", "");
        if (sanitizedBaseName.isBlank()) {
            sanitizedBaseName = "generated-version-" + generatedVersion.getId();
        }
        return sanitizedBaseName + "." + extension;
    }
}
