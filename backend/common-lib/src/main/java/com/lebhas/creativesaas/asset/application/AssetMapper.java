package com.lebhas.creativesaas.asset.application;

import com.lebhas.creativesaas.asset.application.dto.AssetView;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import org.springframework.stereotype.Component;

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
                asset.getFileType(),
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
}
