package com.lebhas.creativesaas.asset.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "upload_sessions", schema = "platform")
public class UploadSessionEntity extends TenantAwareEntity {

    @Column(name = "brand_id")
    private UUID brandId;

    @Column(name = "product_service_id")
    private UUID productServiceId;

    @Column(name = "project_id", updatable = false)
    private UUID projectId;

    @Column(name = "asset_id")
    private UUID assetId;

    @Column(name = "uploaded_by", nullable = false, updatable = false)
    private UUID uploadedBy;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "mime_type", length = 120)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "hash", length = 128)
    private String hash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private UploadSessionStatus status;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @Column(name = "completed_chunk_count", nullable = false)
    private int completedChunkCount;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    protected UploadSessionEntity() {
    }

    public static UploadSessionEntity create(
            UUID workspaceId,
            UUID brandId,
            UUID productServiceId,
            UUID projectId,
            UUID uploadedBy,
            String originalFileName,
            String mimeType,
            long fileSize,
            String hash,
            int chunkCount
    ) {
        UploadSessionEntity entity = new UploadSessionEntity();
        entity.assignWorkspace(workspaceId);
        entity.brandId = brandId;
        entity.productServiceId = productServiceId;
        entity.projectId = projectId;
        entity.uploadedBy = require(uploadedBy, "uploadedBy");
        entity.originalFileName = normalizeRequired(originalFileName, "originalFileName");
        entity.mimeType = normalizeNullable(mimeType);
        entity.fileSize = Math.max(fileSize, 0);
        entity.hash = normalizeNullable(hash);
        entity.status = UploadSessionStatus.PENDING;
        entity.chunkCount = Math.max(chunkCount, 1);
        entity.completedChunkCount = 0;
        return entity;
    }

    public UUID getBrandId() {
        return brandId;
    }

    public UUID getProductServiceId() {
        return productServiceId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getAssetId() {
        return assetId;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getHash() {
        return hash;
    }

    public UploadSessionStatus getStatus() {
        return status;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public int getCompletedChunkCount() {
        return completedChunkCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void attachAsset(UUID assetId) {
        this.assetId = assetId;
    }

    public void markUploading() {
        this.status = UploadSessionStatus.UPLOADING;
        this.errorMessage = null;
    }

    public void markChunkCompleted(int completedChunkCount) {
        this.completedChunkCount = Math.max(0, Math.min(chunkCount, completedChunkCount));
        if (this.completedChunkCount > 0 && this.status == UploadSessionStatus.PENDING) {
            this.status = UploadSessionStatus.UPLOADING;
        }
    }

    public void markCompleted() {
        this.completedChunkCount = this.chunkCount;
        this.status = UploadSessionStatus.COMPLETED;
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = UploadSessionStatus.FAILED;
        this.errorMessage = normalizeNullable(errorMessage);
    }

    private static UUID require(UUID value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
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
