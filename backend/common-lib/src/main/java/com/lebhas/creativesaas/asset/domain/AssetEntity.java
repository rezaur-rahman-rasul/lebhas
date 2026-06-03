package com.lebhas.creativesaas.asset.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "assets", schema = "platform")
public class AssetEntity extends TenantAwareEntity {

    @Column(name = "brand_id")
    private UUID brandId;

    @Column(name = "product_service_id")
    private UUID productServiceId;

    @Column(name = "project_id", updatable = false)
    private UUID projectId;

    @Column(name = "source_type", length = 40)
    private String sourceType;

    @Column(name = "uploaded_by", nullable = false, updatable = false)
    private UUID uploadedBy;

    @Column(name = "folder_id")
    private UUID folderId;

    @Column(name = "storage_file_id")
    private UUID storageFileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 30)
    private AssetType assetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_category", nullable = false, length = 40)
    private AssetCategory assetCategory;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "stored_file_name", length = 255)
    private String storedFileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", length = 30)
    private AssetFileType fileType;

    @Column(name = "mime_type", length = 120)
    private String mimeType;

    @Column(name = "file_extension", length = 20)
    private String fileExtension;

    @Column(name = "file_size")
    private long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider", nullable = false, length = 30)
    private StorageProvider storageProvider;

    @Column(name = "storage_bucket", length = 160)
    private String storageBucket;

    @Column(name = "storage_key", length = 600)
    private String storageKey;

    @Column(name = "checksum", length = 128)
    private String checksum;

    @Column(name = "public_url", length = 1000)
    private String publicUrl;

    @Column(name = "preview_url", length = 1000)
    private String previewUrl;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Column(name = "upload_session_id")
    private UUID uploadSessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "preview_status", nullable = false, length = 30)
    private PreviewStatus previewStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 30)
    private ProcessingStatus processingStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AssetStatus status;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "duration")
    private Long duration;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "asset_tags",
            schema = "platform",
            joinColumns = @JoinColumn(name = "asset_id", nullable = false)
    )
    @Column(name = "tag", nullable = false, length = 80)
    private Set<String> tags = new LinkedHashSet<>();

    @Column(name = "metadata")
    private String metadataJson;

    protected AssetEntity() {
    }

    public static AssetEntity createUploading(
            UUID workspaceId,
            UUID brandId,
            UUID productServiceId,
            UUID projectId,
            UUID uploadedBy,
            UUID folderId,
            AssetType assetType,
            AssetCategory assetCategory,
            String originalFileName,
            String displayName,
            String description,
            Set<String> tags,
            UUID uploadSessionId,
            String metadataJson,
            StorageProvider storageProvider
    ) {
        AssetEntity asset = new AssetEntity();
        asset.assignWorkspace(workspaceId);
        asset.brandId = brandId;
        asset.productServiceId = productServiceId;
        asset.projectId = projectId;
        asset.uploadedBy = require(uploadedBy, "uploadedBy");
        asset.folderId = folderId;
        asset.assetType = assetType == null ? AssetType.RAW : assetType;
        asset.assetCategory = assetCategory == null ? AssetCategory.OTHER : assetCategory;
        asset.originalFileName = normalizeRequired(originalFileName, "originalFileName");
        asset.displayName = normalizeNullable(displayName);
        if (asset.displayName == null) {
            asset.displayName = asset.originalFileName;
        }
        asset.description = normalizeNullable(description);
        asset.tags = new LinkedHashSet<>(tags == null ? Set.of() : tags);
        asset.uploadSessionId = uploadSessionId;
        asset.metadataJson = normalizeNullable(metadataJson);
        asset.storageProvider = storageProvider == null ? StorageProvider.LOCAL : storageProvider;
        asset.previewStatus = PreviewStatus.PENDING;
        asset.processingStatus = ProcessingStatus.UPLOADING;
        asset.status = AssetStatus.UPLOADING;
        return asset;
    }

    public static AssetEntity createSignedUploadPending(
            UUID workspaceId,
            UUID brandId,
            UUID productServiceId,
            UUID projectId,
            UUID uploadedBy,
            UUID folderId,
            AssetType assetType,
            AssetCategory assetCategory,
            String originalFileName,
            String safeFileName,
            String displayName,
            String description,
            Set<String> tags,
            UUID uploadSessionId,
            String metadataJson,
            StorageProvider storageProvider,
            String bucket,
            String objectKey,
            String mimeType,
            String fileExtension,
            long fileSize,
            String checksum,
            String sourceType
    ) {
        AssetEntity asset = createUploading(
                workspaceId,
                brandId,
                productServiceId,
                projectId,
                uploadedBy,
                folderId,
                assetType,
                assetCategory,
                originalFileName,
                displayName,
                description,
                tags,
                uploadSessionId,
                metadataJson,
                storageProvider);
        asset.storedFileName = normalizeNullable(safeFileName);
        asset.mimeType = normalizeNullable(mimeType);
        asset.fileExtension = normalizeNullable(fileExtension);
        asset.fileSize = Math.max(fileSize, 0);
        asset.storageBucket = normalizeNullable(bucket);
        asset.storageKey = normalizeNullable(objectKey);
        asset.checksum = normalizeNullable(checksum);
        asset.sourceType = normalizeNullable(sourceType);
        asset.status = AssetStatus.UPLOAD_PENDING;
        asset.processingStatus = ProcessingStatus.UPLOADING;
        asset.previewStatus = PreviewStatus.PENDING;
        return asset;
    }

    public void configureSignedUploadStorage(String bucket, String objectKey) {
        this.storageBucket = normalizeNullable(bucket);
        this.storageKey = normalizeRequired(objectKey, "objectKey");
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

    public UUID getProjectCampaignId() {
        return projectId;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public String getSourceType() {
        return sourceType;
    }

    public UUID getFolderId() {
        return folderId;
    }

    public UUID getStorageFileId() {
        return storageFileId;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public AssetCategory getAssetCategory() {
        return assetCategory;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public AssetFileType getFileType() {
        return fileType;
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

    public StorageProvider getStorageProvider() {
        return storageProvider;
    }

    public String getStorageBucket() {
        return storageBucket;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public UUID getUploadSessionId() {
        return uploadSessionId;
    }

    public PreviewStatus getPreviewStatus() {
        return previewStatus;
    }

    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public AssetStatus getStatus() {
        return status;
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

    public Set<String> getTags() {
        return Set.copyOf(tags);
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public boolean isReady() {
        return status == AssetStatus.READY || status == AssetStatus.AVAILABLE;
    }

    public void confirmSignedUpload(UUID storageFileId, Integer width, Integer height, Long duration) {
        this.storageFileId = storageFileId;
        this.width = width;
        this.height = height;
        this.duration = duration;
        this.processingStatus = ProcessingStatus.READY;
        this.previewStatus = PreviewStatus.READY;
        this.status = AssetStatus.AVAILABLE;
    }

    public void completeUpload(
            String storedFileName,
            AssetFileType fileType,
            String mimeType,
            String fileExtension,
            long fileSize,
            StorageProvider storageProvider,
            String storageBucket,
            String storageKey,
            String publicUrl,
            String previewUrl,
            String thumbnailUrl,
            Integer width,
            Integer height,
            Long duration
    ) {
        completeUpload(
                storedFileName,
                fileType,
                mimeType,
                fileExtension,
                fileSize,
                storageProvider,
                storageBucket,
                storageKey,
                null,
                publicUrl,
                previewUrl,
                thumbnailUrl,
                width,
                height,
                duration);
    }

    public void completeUpload(
            String storedFileName,
            AssetFileType fileType,
            String mimeType,
            String fileExtension,
            long fileSize,
            StorageProvider storageProvider,
            String storageBucket,
            String storageKey,
            String checksum,
            String publicUrl,
            String previewUrl,
            String thumbnailUrl,
            Integer width,
            Integer height,
            Long duration
    ) {
        this.storedFileName = normalizeNullable(storedFileName);
        this.fileType = fileType;
        this.mimeType = normalizeNullable(mimeType);
        this.fileExtension = normalizeNullable(fileExtension);
        this.fileSize = Math.max(fileSize, 0);
        this.storageProvider = storageProvider == null ? this.storageProvider : storageProvider;
        this.storageBucket = normalizeNullable(storageBucket);
        this.storageKey = normalizeNullable(storageKey);
        this.checksum = normalizeNullable(checksum);
        this.publicUrl = normalizeNullable(publicUrl);
        this.previewUrl = normalizeNullable(previewUrl);
        this.thumbnailUrl = normalizeNullable(thumbnailUrl);
        this.width = width;
        this.height = height;
        this.duration = duration;
        this.processingStatus = ProcessingStatus.READY;
        this.previewStatus = PreviewStatus.READY;
        this.status = AssetStatus.READY;
    }

    public void attachStorageFile(UUID storageFileId) {
        this.storageFileId = storageFileId;
    }

    public void recordChecksum(String checksum) {
        this.checksum = normalizeNullable(checksum);
    }

    public void updateDetails(
            String displayName,
            String description,
            AssetCategory assetCategory,
            Set<String> tags,
            String metadataJson
    ) {
        this.displayName = normalizeNullable(displayName);
        if (this.displayName == null) {
            this.displayName = this.originalFileName;
        }
        this.description = normalizeNullable(description);
        if (assetCategory != null) {
            this.assetCategory = assetCategory;
        }
        this.tags.clear();
        this.tags.addAll(tags == null ? Set.of() : tags);
        this.metadataJson = normalizeNullable(metadataJson);
    }

    public void markPreviewProcessing() {
        this.previewStatus = PreviewStatus.PROCESSING;
        this.processingStatus = ProcessingStatus.PROCESSING;
    }

    public void markPreviewReady() {
        this.previewStatus = PreviewStatus.READY;
        if (this.status == AssetStatus.UPLOADING) {
            this.processingStatus = ProcessingStatus.READY;
            this.status = AssetStatus.READY;
        }
    }

    public void markPreviewFailed() {
        this.previewStatus = PreviewStatus.FAILED;
    }

    public void markUploadFailed() {
        this.status = AssetStatus.FAILED;
        this.processingStatus = ProcessingStatus.FAILED;
        this.previewStatus = PreviewStatus.FAILED;
    }

    public void markDeletedAsset() {
        this.status = AssetStatus.DELETED;
        markDeleted();
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
