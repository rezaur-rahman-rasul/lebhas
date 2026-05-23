package com.lebhas.creativesaas.asset.cache.dto;

import com.lebhas.creativesaas.asset.application.dto.AssetView;
import com.lebhas.creativesaas.common.api.PagedResult;

import java.time.Instant;
import java.util.UUID;

public record AssetListCacheEntry(
        UUID workspaceId,
        UUID projectId,
        int page,
        String criteriaSignature,
        PagedResult<AssetView> result,
        Instant cachedAt
) {
}
