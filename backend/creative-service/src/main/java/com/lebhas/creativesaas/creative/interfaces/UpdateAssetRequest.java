package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.asset.domain.AssetCategory;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record UpdateAssetRequest(
        String displayName,
        String description,
        AssetCategory assetCategory,
        Set<String> tags,
        Map<String, Object> metadata
) {
}
