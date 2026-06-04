package com.lebhas.creativesaas.asset.storage;

import com.lebhas.creativesaas.asset.domain.AssetEntity;
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
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class R2StorageProvider implements StorageService, StorageProvider {

    private static final String GENERATED_VARIANT_TYPE = "generated";

    private final StorageProperties storageProperties;
    private final R2StorageProperties r2StorageProperties;
    private final StoragePathBuilder storagePathBuilder;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final Clock clock;

    public R2StorageProvider(
            StorageProperties storageProperties,
            R2StorageProperties r2StorageProperties,
            StoragePathBuilder storagePathBuilder,
            S3Client s3Client,
            S3Presigner s3Presigner,
            Clock clock
    ) {
        this.storageProperties = storageProperties;
        this.r2StorageProperties = r2StorageProperties;
        this.storagePathBuilder = storagePathBuilder;
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.clock = clock;
    }

    @Override
    public com.lebhas.creativesaas.asset.domain.StorageProvider providerType() {
        return com.lebhas.creativesaas.asset.domain.StorageProvider.R2;
    }

    @Override
    public com.lebhas.creativesaas.asset.domain.StorageProvider provider() {
        return providerType();
    }

    @Override
    public StorageObjectResponse upload(StorageObjectRequest request) {
        requireConfigured();
        String bucket = resolveBucket(request.bucket());
        String objectKey = resolveObjectKey(request);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(request.mimeType())
                .build();
        try (InputStream inputStream = request.contentStreamSupplier().openStream()) {
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, request.contentLength()));
            return new StorageObjectResponse(
                    providerType(),
                    bucket,
                    objectKey,
                    request.fileName(),
                    resolveCdnUrl(objectKey));
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.ASSET_STORAGE_FAILURE, "Cloudflare R2 upload failed");
        }
    }

    @Override
    public void delete(String bucket, String objectKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(resolveBucket(bucket))
                    .key(objectKey)
                    .build());
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.ASSET_STORAGE_FAILURE, "Cloudflare R2 deletion failed");
        }
    }

    @Override
    public SignedUrlResponse generateSignedUrl(SignedUrlRequest request) {
        requireConfigured();
        Instant expiresAt = clock.instant().plus(request.ttl());
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(resolveBucket(request.bucket()))
                .key(request.objectKey())
                .responseContentDisposition(contentDisposition(request))
                .responseContentType(request.mimeType())
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(request.ttl())
                .getObjectRequest(getObjectRequest)
                .build();
        try {
            return new SignedUrlResponse(
                    s3Presigner.presignGetObject(presignRequest).url().toString(),
                    expiresAt,
                    resolveCdnUrl(request.objectKey()));
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.ASSET_STORAGE_FAILURE, "Cloudflare R2 signed URL could not be generated");
        }
    }

    @Override
    public StoredObjectMetadata getMetadata(String bucket, String objectKey) {
        try {
            HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(resolveBucket(bucket))
                    .key(objectKey)
                    .build());
            return new StoredObjectMetadata(response.contentLength(), response.lastModified());
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.ASSET_STORAGE_FAILURE, "Cloudflare R2 metadata could not be read");
        }
    }

    @Override
    public StoredObject store(StorageUploadRequest request) {
        StorageObjectResponse uploaded = upload(new StorageObjectRequest(
                request.workspaceId(),
                request.projectId(),
                request.assetId(),
                null,
                request.storedFileName(),
                request.mimeType(),
                request.file().getSize(),
                null,
                request.file()::getInputStream));
        return new StoredObject(
                uploaded.fileName(),
                uploaded.bucket(),
                uploaded.objectKey(),
                uploaded.cdnUrl(),
                uploaded.cdnUrl(),
                null);
    }

    @Override
    public StoredObject storeGenerated(GeneratedStorageUploadRequest request) {
        String fileName = request.outputId() + "." + request.fileExtension();
        StorageObjectResponse uploaded = upload(new StorageObjectRequest(
                request.workspaceId(),
                request.projectId(),
                request.outputId(),
                GENERATED_VARIANT_TYPE,
                fileName,
                request.mimeType(),
                request.content().length,
                null,
                () -> new ByteArrayInputStream(request.content())));
        return new StoredObject(
                fileName,
                uploaded.bucket(),
                uploaded.objectKey(),
                uploaded.cdnUrl(),
                uploaded.cdnUrl(),
                null);
    }

    @Override
    public SignedAssetUrl generatePreviewUrl(AssetEntity asset) {
        SignedUrlResponse response = generateSignedUrl(new SignedUrlRequest(
                asset.getStorageBucket(),
                asset.getStorageKey(),
                storageProperties.getSignedUrlTtl(),
                asset.getOriginalFileName(),
                false,
                asset.getMimeType()));
        return new SignedAssetUrl(response.url(), response.expiresAt());
    }

    @Override
    public SignedAssetUrl generateDownloadUrl(AssetEntity asset) {
        SignedUrlResponse response = generateSignedUrl(new SignedUrlRequest(
                asset.getStorageBucket(),
                asset.getStorageKey(),
                storageProperties.getSignedUrlTtl(),
                asset.getOriginalFileName(),
                true,
                asset.getMimeType()));
        return new SignedAssetUrl(response.url(), response.expiresAt());
    }

    @Override
    public SignedAssetUrl generateUploadUrl(
            String bucket,
            String objectKey,
            String mimeType,
            long contentLength,
            Duration ttl
    ) {
        requireConfigured();
        Duration effectiveTtl = ttl == null || ttl.isZero() || ttl.isNegative()
                ? storageProperties.getSignedUrlTtl()
                : ttl;
        Instant expiresAt = clock.instant().plus(effectiveTtl);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(resolveBucket(bucket))
                .key(objectKey)
                .build();
        PutObjectPresignRequest request = PutObjectPresignRequest.builder()
                .signatureDuration(effectiveTtl)
                .putObjectRequest(putObjectRequest)
                .build();
        try {
            return new SignedAssetUrl(s3Presigner.presignPutObject(request).url().toString(), expiresAt);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.ASSET_STORAGE_FAILURE, "Cloudflare R2 signed upload URL could not be generated");
        }
    }

    @Override
    public void delete(AssetEntity asset) {
        delete(asset.getStorageBucket(), asset.getStorageKey());
    }

    @Override
    public StoredObjectMetadata getMetadata(AssetEntity asset) {
        return getMetadata(asset.getStorageBucket(), asset.getStorageKey());
    }

    @Override
    public byte[] readBytes(AssetEntity asset) {
        try {
            return s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(resolveBucket(asset.getStorageBucket()))
                    .key(asset.getStorageKey())
                    .build()).asByteArray();
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.ASSET_STORAGE_FAILURE, "Cloudflare R2 asset content could not be read");
        }
    }

    private String resolveObjectKey(StorageObjectRequest request) {
        if (request.variantType() != null && !request.variantType().isBlank()) {
            return storagePathBuilder.buildVariantPath(
                    request.workspaceId(),
                    request.assetId(),
                    request.variantType(),
                    request.fileName());
        }
        if (request.projectId() == null) {
            return storagePathBuilder.buildWorkspaceAssetPath(
                    request.workspaceId(),
                    request.assetId(),
                    request.fileName());
        }
        return storagePathBuilder.buildAssetPath(
                request.workspaceId(),
                request.projectId(),
                request.assetId(),
                request.fileName());
    }

    private String resolveBucket(String requestedBucket) {
        if (requestedBucket != null && !requestedBucket.isBlank()) {
            return requestedBucket.trim();
        }
        if (r2StorageProperties.getBucket() != null && !r2StorageProperties.getBucket().isBlank()) {
            return r2StorageProperties.getBucket().trim();
        }
        return storageProperties.getBucket();
    }

    private void requireConfigured() {
        List<String> missing = new ArrayList<>();
        if (r2StorageProperties.getEndpoint() == null) {
            missing.add("R2_ENDPOINT");
        }
        if (r2StorageProperties.getAccessKey() == null || r2StorageProperties.getAccessKey().isBlank()) {
            missing.add("R2_ACCESS_KEY");
        }
        if (r2StorageProperties.getSecretKey() == null || r2StorageProperties.getSecretKey().isBlank()) {
            missing.add("R2_SECRET_KEY");
        }
        if (resolveBucket(null) == null || resolveBucket(null).isBlank()) {
            missing.add("R2_BUCKET");
        }
        if (!missing.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.ASSET_STORAGE_FAILURE,
                    "Cloudflare R2 storage is not configured. Missing: " + String.join(", ", missing));
        }
    }

    private String resolveCdnUrl(String objectKey) {
        if (r2StorageProperties.getPublicBaseUrl() == null) {
            return null;
        }
        return r2StorageProperties.getPublicBaseUrl().toString().replaceAll("/+$", "") + "/" + objectKey;
    }

    private String contentDisposition(SignedUrlRequest request) {
        if (!request.download()) {
            return "inline";
        }
        if (request.fileName() == null || request.fileName().isBlank()) {
            return "attachment";
        }
        return "attachment; filename=\"" + request.fileName().replace("\"", "") + "\"";
    }
}
