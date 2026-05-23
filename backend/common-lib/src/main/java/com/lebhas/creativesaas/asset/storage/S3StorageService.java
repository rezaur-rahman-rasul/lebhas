package com.lebhas.creativesaas.asset.storage;

import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.domain.StorageProvider;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.time.Instant;

public class S3StorageService implements StorageService {

    private final StorageProvider provider;
    private final StorageProperties storageProperties;
    private final StoragePathResolver storagePathResolver;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public S3StorageService(
            StorageProvider provider,
            StorageProperties storageProperties,
            StoragePathResolver storagePathResolver,
            S3Client s3Client,
            S3Presigner s3Presigner
    ) {
        this.provider = provider;
        this.storageProperties = storageProperties;
        this.storagePathResolver = storagePathResolver;
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    @Override
    public StorageProvider provider() {
        return provider;
    }

    @Override
    public StoredObject store(StorageUploadRequest request) {
        String storageKey = storagePathResolver.resolveRaw(
                request.workspaceId(),
                request.projectId(),
                request.assetId());
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(storageProperties.getBucket())
                .key(storageKey)
                .contentType(request.mimeType())
                .build();
        try (InputStream inputStream = request.file().getInputStream()) {
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, request.file().getSize()));
            return new StoredObject(
                    request.storedFileName(),
                    storageProperties.getBucket(),
                    storageKey,
                    null,
                    null,
                    null);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.ASSET_STORAGE_FAILURE, "S3 asset upload failed");
        }
    }

    @Override
    public StoredObject storeGenerated(GeneratedStorageUploadRequest request) {
        String storageKey = storagePathResolver.resolveGenerated(
                request.workspaceId(),
                request.projectId(),
                request.outputId());
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(storageProperties.getBucket())
                .key(storageKey)
                .contentType(request.mimeType())
                .build();
        try {
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(request.content()));
            return new StoredObject(
                    request.outputId() + "." + request.fileExtension(),
                    storageProperties.getBucket(),
                    storageKey,
                    null,
                    null,
                    null);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.ASSET_STORAGE_FAILURE, "S3 generated creative upload failed");
        }
    }

    @Override
    public SignedAssetUrl generatePreviewUrl(AssetEntity asset) {
        return presign(asset, "inline");
    }

    @Override
    public SignedAssetUrl generateDownloadUrl(AssetEntity asset) {
        return presign(asset, "attachment; filename=\"" + asset.getOriginalFileName().replace("\"", "") + "\"");
    }

    @Override
    public void delete(AssetEntity asset) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(asset.getStorageBucket())
                    .key(asset.getStorageKey())
                    .build());
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.ASSET_STORAGE_FAILURE, "S3 asset deletion failed");
        }
    }

    @Override
    public StoredObjectMetadata getMetadata(AssetEntity asset) {
        try {
            HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(asset.getStorageBucket())
                    .key(asset.getStorageKey())
                    .build());
            return new StoredObjectMetadata(response.contentLength(), response.lastModified());
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.ASSET_STORAGE_FAILURE, "S3 asset metadata could not be read");
        }
    }

    private SignedAssetUrl presign(AssetEntity asset, String contentDisposition) {
        Instant expiresAt = Instant.now().plus(storageProperties.getSignedUrlTtl());
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(asset.getStorageBucket())
                .key(asset.getStorageKey())
                .responseContentDisposition(contentDisposition)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(storageProperties.getSignedUrlTtl())
                .getObjectRequest(getObjectRequest)
                .build();
        return new SignedAssetUrl(
                s3Presigner.presignGetObject(presignRequest).url().toString(),
                expiresAt);
    }
}
