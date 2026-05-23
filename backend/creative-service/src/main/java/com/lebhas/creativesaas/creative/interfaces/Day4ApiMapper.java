package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.asset.application.AssetMetadataSerializer;
import com.lebhas.creativesaas.asset.application.dto.AssetListCriteria;
import com.lebhas.creativesaas.asset.application.dto.AssetUrlView;
import com.lebhas.creativesaas.asset.application.dto.AssetView;
import com.lebhas.creativesaas.asset.application.dto.UploadAssetCommand;
import com.lebhas.creativesaas.common.api.PagedResult;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Component
public class Day4ApiMapper {

    private final AssetMetadataSerializer assetMetadataSerializer;
    public Day4ApiMapper(AssetMetadataSerializer assetMetadataSerializer) {
        this.assetMetadataSerializer = assetMetadataSerializer;
    }

    public UploadAssetCommand toUploadAssetCommand(UUID workspaceId, AssetUploadApiRequest request) {
        return new UploadAssetCommand(
                workspaceId,
                request.getProjectId(),
                request.getAssetCategory(),
                request.getDisplayName(),
                request.getDescription(),
                parseTags(request.getTags()),
                assetMetadataSerializer.deserialize(request.getMetadata()),
                request.getFile());
    }

    public AssetListCriteria toAssetListCriteria(UUID workspaceId, UUID projectId, ProjectAssetListApiRequest request) {
        return new AssetListCriteria(
                workspaceId,
                projectId,
                request.getAssetType(),
                request.getAssetCategory(),
                request.getPreviewStatus(),
                request.getProcessingStatus(),
                request.getUploadedBy(),
                request.getStatus(),
                request.getSearch(),
                request.getCreatedFrom(),
                request.getCreatedTo(),
                request.getPage() == null ? 0 : request.getPage(),
                request.getSize() == null ? 20 : request.getSize(),
                request.getSortBy(),
                request.getDirection());
    }

    public AssetResponse toAssetResponse(AssetView view) {
        return new AssetResponse(
                view.id(),
                view.workspaceId(),
                view.brandId(),
                view.productServiceId(),
                view.projectCampaignId(),
                view.storageFileId(),
                view.uploadedBy(),
                view.assetType(),
                view.assetCategory(),
                view.originalFileName(),
                view.displayName(),
                view.description(),
                view.uploadSessionId(),
                view.previewStatus(),
                view.processingStatus(),
                view.status(),
                view.tags(),
                view.metadata(),
                view.createdAt(),
                view.updatedAt());
    }

    public PagedResult<AssetResponse> toAssetPage(PagedResult<AssetView> page) {
        return new PagedResult<>(
                page.items().stream().map(this::toAssetResponse).toList(),
                page.totalItems(),
                page.totalPages(),
                page.page(),
                page.size(),
                page.first(),
                page.last());
    }

    public AssetDownloadResponse toAssetDownloadResponse(UUID assetId, AssetUrlView view) {
        return new AssetDownloadResponse(
                assetId,
                view.url(),
                view.type(),
                view.cdnUrl(),
                view.cached(),
                view.generatedAt(),
                view.expiresAt());
    }

    private Set<String> parseTags(String rawTags) {
        if (rawTags == null || rawTags.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(rawTags.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }
}
