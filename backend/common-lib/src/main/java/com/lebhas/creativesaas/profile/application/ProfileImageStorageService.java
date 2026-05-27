package com.lebhas.creativesaas.profile.application;

import com.lebhas.creativesaas.asset.storage.R2StorageProperties;
import com.lebhas.creativesaas.asset.storage.StorageProperties;
import com.lebhas.creativesaas.asset.storage.StorageService;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class ProfileImageStorageService {

    private final R2StorageProperties r2StorageProperties;
    private final StorageProperties storageProperties;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final Clock clock;
    private final Duration uploadUrlTtl;

    public ProfileImageStorageService(
            R2StorageProperties r2StorageProperties,
            StorageProperties storageProperties,
            S3Client s3Client,
            S3Presigner s3Presigner,
            Clock clock,
            @Value("${platform.profile.image.upload-url-ttl:PT15M}") Duration uploadUrlTtl
    ) {
        this.r2StorageProperties = r2StorageProperties;
        this.storageProperties = storageProperties;
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.clock = clock;
        this.uploadUrlTtl = uploadUrlTtl == null || uploadUrlTtl.isNegative() || uploadUrlTtl.isZero()
                ? Duration.ofMinutes(15)
                : uploadUrlTtl;
    }

    public SignedProfileImageUrl createUploadUrl(String objectKey, String mimeType, long fileSize) {
        Instant expiresAt = clock.instant().plus(uploadUrlTtl);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket())
                .key(objectKey)
                .contentType(mimeType)
                .contentLength(fileSize)
                .build();
        PutObjectPresignRequest request = PutObjectPresignRequest.builder()
                .signatureDuration(uploadUrlTtl)
                .putObjectRequest(putObjectRequest)
                .build();
        return new SignedProfileImageUrl(s3Presigner.presignPutObject(request).url().toString(), expiresAt);
    }

    public SignedProfileImageUrl createPreviewUrl(String objectKey, String mimeType) {
        Duration ttl = storageProperties.getSignedUrlTtl();
        Instant expiresAt = clock.instant().plus(ttl);
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket())
                .key(objectKey)
                .responseContentDisposition("inline")
                .responseContentType(mimeType)
                .build();
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(getObjectRequest)
                .build();
        return new SignedProfileImageUrl(s3Presigner.presignGetObject(request).url().toString(), expiresAt);
    }

    public StorageService.StoredObjectMetadata metadata(String objectKey) {
        try {
            HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket())
                    .key(objectKey)
                    .build());
            return new StorageService.StoredObjectMetadata(response.contentLength(), response.lastModified());
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.ASSET_METADATA_INVALID, "Profile image upload could not be confirmed");
        }
    }

    public void deleteQuietly(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket())
                    .key(objectKey)
                    .build());
        } catch (RuntimeException ignored) {
        }
    }

    private String bucket() {
        if (r2StorageProperties.getBucket() != null && !r2StorageProperties.getBucket().isBlank()) {
            return r2StorageProperties.getBucket().trim();
        }
        return storageProperties.getBucket();
    }

    public record SignedProfileImageUrl(String url, Instant expiresAt) {
    }
}
