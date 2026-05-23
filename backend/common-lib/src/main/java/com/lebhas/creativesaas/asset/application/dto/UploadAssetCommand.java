package com.lebhas.creativesaas.asset.application.dto;

import com.lebhas.creativesaas.asset.domain.AssetCategory;
import com.lebhas.creativesaas.asset.domain.AssetType;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record UploadAssetCommand(
        UUID workspaceId,
        UUID projectId,
        AssetType assetType,
        AssetCategory assetCategory,
        String displayName,
        String description,
        Set<String> tags,
        Map<String, Object> metadata,
        MultipartFile file
) {
    public UploadAssetCommand(
            UUID workspaceId,
            UUID projectId,
            AssetCategory assetCategory,
            String displayName,
            String description,
            Set<String> tags,
            Map<String, Object> metadata,
            MultipartFile file
    ) {
        this(workspaceId, projectId, null, assetCategory, displayName, description, tags, metadata, file);
    }
}
