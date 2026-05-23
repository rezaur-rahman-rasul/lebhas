package com.lebhas.creativesaas.asset.application;

import com.lebhas.creativesaas.asset.application.dto.AssetUrlView;
import com.lebhas.creativesaas.asset.cache.AssetSignedUrlRedisCacheService;
import com.lebhas.creativesaas.asset.cache.dto.AssetSignedUrlCacheEntry;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.storage.application.StorageFileService;
import com.lebhas.creativesaas.storage.domain.StorageFileEntity;
import com.lebhas.creativesaas.asset.storage.StorageService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class SignedUrlService {

    private final StorageService storageService;
    private final StorageFileService storageFileService;
    private final AssetSignedUrlRedisCacheService assetSignedUrlRedisCacheService;
    private final AssetEventPublisher assetEventPublisher;

    public SignedUrlService(
            StorageService storageService,
            StorageFileService storageFileService,
            AssetSignedUrlRedisCacheService assetSignedUrlRedisCacheService,
            AssetEventPublisher assetEventPublisher
    ) {
        this.storageService = storageService;
        this.storageFileService = storageFileService;
        this.assetSignedUrlRedisCacheService = assetSignedUrlRedisCacheService;
        this.assetEventPublisher = assetEventPublisher;
    }

    public AssetUrlView previewUrl(AssetEntity asset) {
        if (asset.getPreviewStatus() != com.lebhas.creativesaas.asset.domain.PreviewStatus.READY) {
            throw new BusinessException(ErrorCode.ASSET_PREVIEW_NOT_READY);
        }
        return resolve(asset, "preview", () -> storageService.generatePreviewUrl(asset));
    }

    public AssetUrlView downloadUrl(AssetEntity asset) {
        return resolve(asset, "download", () -> storageService.generateDownloadUrl(asset));
    }

    public void invalidate(AssetEntity asset) {
        assetSignedUrlRedisCacheService.invalidate(asset.getWorkspaceId(), asset.getId());
    }

    private AssetUrlView resolve(
            AssetEntity asset,
            String type,
            java.util.function.Supplier<StorageService.SignedAssetUrl> loader
    ) {
        StorageFileEntity storageFile = requireStorageFile(asset);
        AssetSignedUrlCacheEntry cached = assetSignedUrlRedisCacheService.get(asset.getWorkspaceId(), asset.getId(), type)
                .filter(snapshot -> snapshot.expiresAt() != null && snapshot.expiresAt().isAfter(Instant.now()))
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

        StorageService.SignedAssetUrl signedAssetUrl = loader.get();
        Instant generatedAt = Instant.now();
        assetSignedUrlRedisCacheService.store(
                asset.getWorkspaceId(),
                asset.getId(),
                new AssetSignedUrlCacheEntry(
                        asset.getId(),
                        storageFile.getId(),
                        type,
                        signedAssetUrl.url(),
                        storageFile.getCdnUrl(),
                        generatedAt,
                        signedAssetUrl.expiresAt()));
        assetEventPublisher.publish(
                KafkaTopicConstants.SIGNED_URL_GENERATED,
                asset.getWorkspaceId(),
                asset.getId(),
                Map.of(
                        "workspaceId", asset.getWorkspaceId().toString(),
                        "assetId", asset.getId().toString(),
                        "storageFileId", storageFile.getId().toString(),
                        "type", type));
        return new AssetUrlView(
                signedAssetUrl.url(),
                type,
                storageFile.getCdnUrl(),
                false,
                generatedAt,
                signedAssetUrl.expiresAt());
    }

    private StorageFileEntity requireStorageFile(AssetEntity asset) {
        if (asset.getStorageFileId() == null) {
            throw new BusinessException(ErrorCode.STORAGE_FILE_NOT_FOUND);
        }
        return storageFileService.requireStorageFile(asset.getWorkspaceId(), asset.getStorageFileId());
    }
}
