package com.lebhas.creativesaas.asset.application;

import com.lebhas.creativesaas.asset.application.dto.AssetView;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.domain.AssetFileType;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class AssetMapper {

    private final AssetMetadataSerializer assetMetadataSerializer;

    public AssetMapper(AssetMetadataSerializer assetMetadataSerializer) {
        this.assetMetadataSerializer = assetMetadataSerializer;
    }

    public AssetView toAssetView(AssetEntity asset) {
        return new AssetView(
                asset.getId(),
                asset.getWorkspaceId(),
                asset.getBrandId(),
                asset.getProductServiceId(),
                asset.getProjectCampaignId(),
                asset.getStorageFileId(),
                asset.getUploadedBy(),
                asset.getFolderId(),
                asset.getAssetType(),
                asset.getAssetCategory(),
                asset.getOriginalFileName(),
                asset.getStoredFileName(),
                resolveFileType(asset),
                asset.getMimeType(),
                asset.getFileExtension(),
                asset.getFileSize(),
                asset.getStorageProvider(),
                asset.getStorageBucket(),
                asset.getStorageKey(),
                asset.getPublicUrl(),
                asset.getPreviewUrl(),
                asset.getThumbnailUrl(),
                asset.getDisplayName(),
                asset.getDescription(),
                asset.getUploadSessionId(),
                asset.getPreviewStatus(),
                asset.getProcessingStatus(),
                asset.getStatus(),
                asset.getWidth(),
                asset.getHeight(),
                asset.getDuration(),
                asset.getTags(),
                assetMetadataSerializer.deserialize(asset.getMetadataJson()),
                asset.getCreatedAt(),
                asset.getUpdatedAt());
    }

    private AssetFileType resolveFileType(AssetEntity asset) {
        if (asset.getFileType() != null) {
            return asset.getFileType();
        }
        String mimeType = normalize(asset.getMimeType());
        String extension = normalize(asset.getFileExtension());
        if ("image/svg+xml".equals(mimeType) || "svg".equals(extension)) {
            return AssetFileType.VECTOR_IMAGE;
        }
        if (mimeType.startsWith("video/")
                || extension.equals("mp4")
                || extension.equals("mov")
                || extension.equals("m4v")
                || extension.equals("webm")) {
            return AssetFileType.VIDEO;
        }
        if (mimeType.startsWith("image/")
                || extension.equals("jpg")
                || extension.equals("jpeg")
                || extension.equals("png")
                || extension.equals("webp")
                || extension.equals("gif")
                || extension.equals("avif")) {
            return AssetFileType.IMAGE;
        }
        return AssetFileType.IMAGE;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
