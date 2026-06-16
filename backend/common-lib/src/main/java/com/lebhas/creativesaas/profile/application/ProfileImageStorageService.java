package com.lebhas.creativesaas.profile.application;

import com.lebhas.creativesaas.asset.domain.StorageProvider;
import com.lebhas.creativesaas.asset.storage.R2StorageProperties;
import com.lebhas.creativesaas.asset.storage.StorageProperties;
import com.lebhas.creativesaas.asset.storage.StorageService;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.core.sync.RequestBody;

import java.io.IOException;
import java.io.InputStream;
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
            ObjectProvider<S3Client> s3Client,
            ObjectProvider<S3Presigner> s3Presigner,
            Clock clock,
            @Value("${platform.profile.image.upload-url-ttl:PT15M}") Duration uploadUrlTtl
    ) {
        this.r2StorageProperties = r2StorageProperties;
        this.storageProperties = storageProperties;
        this.s3Client = s3Client.getIfAvailable();
        this.s3Presigner = s3Presigner.getIfAvailable();
        this.clock = clock;
        this.uploadUrlTtl = uploadUrlTtl == null || uploadUrlTtl.isNegative() || uploadUrlTtl.isZero()
                ? Duration.ofMinutes(15)
                : uploadUrlTtl;
    }

    public SignedProfileImageUrl createUploadUrl(String objectKey, String mimeType, long fileSize) {
        requireStorageConfigured();
        Instant expiresAt = clock.instant().plus(uploadUrlTtl);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket())
                .key(objectKey)
                .build();
        PutObjectPresignRequest request = PutObjectPresignRequest.builder()
                .signatureDuration(uploadUrlTtl)
                .putObjectRequest(putObjectRequest)
                .build();
        return new SignedProfileImageUrl(requirePresigner().presignPutObject(request).url().toString(), expiresAt);
    }

    public SignedProfileImageUrl createPreviewUrl(String objectKey, String mimeType) {
        requireStorageConfigured();
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
        return new SignedProfileImageUrl(requirePresigner().presignGetObject(request).url().toString(), expiresAt);
    }

    public void store(String objectKey, String mimeType, MultipartFile file) {
        requireStorageConfigured();
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket())
                .key(objectKey)
                .contentType(mimeType)
                .contentLength(file.getSize())
                .build();
        try (InputStream inputStream = file.getInputStream()) {
            requireClient().putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(inputStream, file.getSize()));
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new BusinessException(ErrorCode.ASSET_STORAGE_FAILURE, "Profile image could not be stored");
        }
    }

    public StorageService.StoredObjectMetadata metadata(String objectKey) {
        requireStorageConfigured();
        try {
            HeadObjectResponse response = requireClient().headObject(HeadObjectRequest.builder()
                    .bucket(bucket())
                    .key(objectKey)
                    .build());
            return new StorageService.StoredObjectMetadata(response.contentLength(), response.lastModified());
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.ASSET_METADATA_INVALID, "Profile image upload could not be confirmed");
        }
    }

    public void deleteQuietly(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        try {
            requireClient().deleteObject(DeleteObjectRequest.builder()
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

    private void requireStorageConfigured() {
        if (storageProperties.getProvider() == StorageProvider.R2) {
            java.util.List<String> missing = new java.util.ArrayList<>();
            if (r2StorageProperties.getEndpoint() == null) {
                missing.add("R2_ENDPOINT");
            }
            if (r2StorageProperties.getAccessKey() == null || r2StorageProperties.getAccessKey().isBlank()) {
                missing.add("R2_ACCESS_KEY");
            }
            if (r2StorageProperties.getSecretKey() == null || r2StorageProperties.getSecretKey().isBlank()) {
                missing.add("R2_SECRET_KEY");
            }
            if (bucket() == null || bucket().isBlank()) {
                missing.add("R2_BUCKET");
            }
            if (!missing.isEmpty()) {
                throw new BusinessException(
                        ErrorCode.ASSET_STORAGE_FAILURE,
                        "Profile image storage is not configured. Missing: " + String.join(", ", missing));
            }
        }
    }

    private S3Client requireClient() {
        if (s3Client == null) {
            throw new BusinessException(ErrorCode.ASSET_STORAGE_FAILURE, "Profile image storage client is not configured");
        }
        return s3Client;
    }

    private S3Presigner requirePresigner() {
        if (s3Presigner == null) {
            throw new BusinessException(ErrorCode.ASSET_STORAGE_FAILURE, "Profile image signed URL storage is not configured");
        }
        return s3Presigner;
    }

    public record SignedProfileImageUrl(String url, Instant expiresAt) {
    }
}
