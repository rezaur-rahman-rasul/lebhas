package com.lebhas.creativesaas.asset.application.dto;

import com.lebhas.creativesaas.asset.domain.AssetCategory;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record UpdateAssetCommand(
        UUID workspaceId,
        UUID assetId,
        String displayName,
        String description,
        AssetCategory assetCategory,
        Set<String> tags,
        Map<String, Object> metadata
) {
}
