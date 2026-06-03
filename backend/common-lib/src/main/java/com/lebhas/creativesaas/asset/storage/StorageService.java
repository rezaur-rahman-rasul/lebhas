package com.lebhas.creativesaas.asset.storage;

import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.domain.StorageProvider;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.Duration;
import java.util.UUID;

public interface StorageService {

    StorageProvider provider();

    StoredObject store(StorageUploadRequest request);

    StoredObject storeGenerated(GeneratedStorageUploadRequest request);

    default byte[] readBytes(AssetEntity asset) {
        throw new UnsupportedOperationException("Asset reads are not supported by this storage provider");
    }

    SignedAssetUrl generatePreviewUrl(AssetEntity asset);

    SignedAssetUrl generateDownloadUrl(AssetEntity asset);

    default SignedAssetUrl generateUploadUrl(
            String bucket,
            String objectKey,
            String mimeType,
            long contentLength,
            Duration ttl
    ) {
        throw new UnsupportedOperationException("Signed upload URLs are not supported by this storage provider");
    }

    void delete(AssetEntity asset);

    StoredObjectMetadata getMetadata(AssetEntity asset);

    record StorageUploadRequest(
            UUID workspaceId,
            UUID projectId,
            UUID assetId,
            String storedFileName,
            String mimeType,
            MultipartFile file
    ) {
    }

    record GeneratedStorageUploadRequest(
            UUID workspaceId,
            UUID projectId,
            UUID outputId,
            String fileExtension,
            String mimeType,
            byte[] content
    ) {
    }

    record StoredObject(
            String storedFileName,
            String bucket,
            String storageKey,
            String publicUrl,
            String previewUrl,
            String thumbnailUrl
    ) {
    }

    record SignedAssetUrl(String url, Instant expiresAt) {
    }

    record StoredObjectMetadata(long contentLength, Instant lastModified) {
    }
}
