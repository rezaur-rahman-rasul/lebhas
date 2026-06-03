package com.lebhas.creativesaas.storage.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "storage_files", schema = "platform")
public class StorageFileEntity extends TenantAwareEntity {

    @Column(name = "project_id", updatable = false)
    private UUID projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 30)
    private com.lebhas.creativesaas.asset.domain.StorageProvider provider;

    @Column(name = "bucket", nullable = false, length = 160)
    private String bucket;

    @Column(name = "object_key", nullable = false, length = 600)
    private String objectKey;

    @Column(name = "cdn_url", length = 1000)
    private String cdnUrl;

    @Column(name = "mime_type", length = 120)
    private String mimeType;

    @Column(name = "file_extension", length = 20)
    private String fileExtension;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "hash", length = 128)
    private String hash;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "duration")
    private Long duration;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_class", nullable = false, length = 40)
    private StorageClass storageClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_purpose", nullable = false, length = 40)
    private StorageFilePurpose filePurpose;

    protected StorageFileEntity() {
    }

    public static StorageFileEntity create(
            UUID workspaceId,
            UUID projectId,
            com.lebhas.creativesaas.asset.domain.StorageProvider provider,
            String bucket,
            String objectKey,
            String cdnUrl,
            String mimeType,
            String fileExtension,
            long fileSize,
            String hash,
            Integer width,
            Integer height,
            Long duration,
            StorageClass storageClass,
            StorageFilePurpose filePurpose
    ) {
        StorageFileEntity file = new StorageFileEntity();
        file.assignWorkspace(workspaceId);
        file.projectId = projectId;
        file.provider = requireProvider(provider);
        file.bucket = normalizeRequired(bucket, "bucket");
        file.objectKey = normalizeRequired(objectKey, "objectKey");
        file.cdnUrl = normalizeNullable(cdnUrl);
        file.mimeType = normalizeNullable(mimeType);
        file.fileExtension = normalizeNullable(fileExtension);
        file.fileSize = Math.max(fileSize, 0);
        file.hash = normalizeNullable(hash);
        file.width = width;
        file.height = height;
        file.duration = duration;
        file.storageClass = storageClass == null ? StorageClass.STANDARD : storageClass;
        file.filePurpose = filePurpose == null ? StorageFilePurpose.RAW : filePurpose;
        return file;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public com.lebhas.creativesaas.asset.domain.StorageProvider getProvider() {
        return provider;
    }

    public String getBucket() {
        return bucket;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getCdnUrl() {
        return cdnUrl;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getHash() {
        return hash;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public Long getDuration() {
        return duration;
    }

    public StorageClass getStorageClass() {
        return storageClass;
    }

    public StorageFilePurpose getFilePurpose() {
        return filePurpose;
    }

    private static com.lebhas.creativesaas.asset.domain.StorageProvider requireProvider(
            com.lebhas.creativesaas.asset.domain.StorageProvider provider
    ) {
        if (provider == null) {
            throw new IllegalArgumentException("provider must not be null");
        }
        return provider;
    }

    private static String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
