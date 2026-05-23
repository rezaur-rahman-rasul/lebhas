package com.lebhas.creativesaas.asset.application.dto;

import com.lebhas.creativesaas.asset.domain.AssetCategory;
import com.lebhas.creativesaas.asset.domain.AssetType;
import com.lebhas.creativesaas.asset.domain.AssetStatus;
import com.lebhas.creativesaas.asset.domain.PreviewStatus;
import com.lebhas.creativesaas.asset.domain.ProcessingStatus;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record AssetView(
        UUID id,
        UUID workspaceId,
        UUID brandId,
        UUID productServiceId,
        UUID projectCampaignId,
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
