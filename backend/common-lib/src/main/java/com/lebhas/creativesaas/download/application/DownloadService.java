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
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.redis.RedisSignedUrlCache;
import com.lebhas.creativesaas.sharing.application.ShareLinkService;
import com.lebhas.creativesaas.storage.application.StorageFileService;
import com.lebhas.creativesaas.storage.domain.StorageFileEntity;
import com.lebhas.creativesaas.usage.application.ShareUsageAccessService;
import com.lebhas.creativesaas.usage.application.dto.ShareUsageTrackingCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
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
    private final Clock clock;

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
        this.clock = clock;
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
    public AssetUrlView requestPublicShareDownload(String token, String password, DownloadRequestContext requestContext) {
        ShareLinkService.ResolvedShareLink shareLink = shareLinkService.resolvePublicShareLink(token, password);
        GeneratedVersionEntity generatedVersion = shareLink.generatedVersion();
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
        return urlView;
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
