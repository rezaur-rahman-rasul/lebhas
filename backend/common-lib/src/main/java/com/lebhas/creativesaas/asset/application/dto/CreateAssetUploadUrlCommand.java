package com.lebhas.creativesaas.asset.application.dto;

import com.lebhas.creativesaas.asset.domain.AssetCategory;
import com.lebhas.creativesaas.asset.domain.AssetType;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record CreateAssetUploadUrlCommand(
        UUID workspaceId,
        UUID projectId,
        AssetType assetType,
        AssetCategory assetCategory,
        UUID folderId,
        String originalFileName,
        String contentType,
        long sizeBytes,
        String checksum,
        String displayName,
        String description,
        Set<String> tags,
        Map<String, Object> metadata
) {
}
