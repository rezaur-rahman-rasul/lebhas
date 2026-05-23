package com.lebhas.creativesaas.asset.cache.dto;

import com.lebhas.creativesaas.asset.application.dto.AssetView;

import java.time.Instant;
import java.util.UUID;

public record AssetMetadataCacheEntry(
        UUID assetId,
        UUID workspaceId,
        UUID projectId,
        AssetView asset,
        Instant cachedAt
) {
}
