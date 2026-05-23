package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.asset.domain.AssetCategory;
import com.lebhas.creativesaas.asset.domain.AssetStatus;
import com.lebhas.creativesaas.asset.domain.AssetType;
import com.lebhas.creativesaas.asset.domain.PreviewStatus;
import com.lebhas.creativesaas.asset.domain.ProcessingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Schema(description = "Asset metadata exposed by the Day 4 API.")
public record AssetResponse(
        UUID id,
        UUID workspaceId,
        UUID brandId,
        UUID productServiceId,
        UUID projectId,
        UUID storageFileId,
        UUID uploadedBy,
        AssetType assetType,
        AssetCategory assetCategory,
        String originalFileName,
        String displayName,
        String description,
        UUID uploadSessionId,
        PreviewStatus previewStatus,
        ProcessingStatus processingStatus,
        AssetStatus status,
        Set<String> tags,
        Map<String, Object> metadata,
        Instant createdAt,
        Instant updatedAt
) {
}
