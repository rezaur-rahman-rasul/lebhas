package com.lebhas.creativesaas.asset.application;

import com.lebhas.creativesaas.asset.application.dto.AssetListCriteria;
import com.lebhas.creativesaas.asset.application.dto.AssetView;
import com.lebhas.creativesaas.asset.cache.AssetMetadataRedisCacheService;
import com.lebhas.creativesaas.common.api.PagedResult;
import com.lebhas.creativesaas.workspace.application.WorkspaceActivityLogger;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.function.Supplier;

@Service
public class AssetCacheService {

    private final AssetMetadataRedisCacheService assetMetadataRedisCacheService;
    private final WorkspaceActivityLogger workspaceActivityLogger;

    public AssetCacheService(
            AssetMetadataRedisCacheService assetMetadataRedisCacheService,
            WorkspaceActivityLogger workspaceActivityLogger
    ) {
        this.assetMetadataRedisCacheService = assetMetadataRedisCacheService;
        this.workspaceActivityLogger = workspaceActivityLogger;
    }

    public AssetView getOrLoadAsset(UUID workspaceId, UUID assetId, Supplier<AssetView> loader) {
        return assetMetadataRedisCacheService.getAsset(workspaceId, assetId)
                .filter(entry -> workspaceId != null && workspaceId.equals(entry.workspaceId()))
                .map(com.lebhas.creativesaas.asset.cache.dto.AssetMetadataCacheEntry::asset)
                .orElseGet(loader);
    }

    public void cacheAsset(AssetView assetView) {
        assetMetadataRedisCacheService.cacheAsset(assetView);
    }

    public PagedResult<AssetView> getOrLoadList(AssetListCriteria criteria, Supplier<PagedResult<AssetView>> loader) {
        return loader.get();
    }

    public void invalidate(UUID workspaceId, UUID projectId, UUID assetId, UUID actorUserId) {
        assetMetadataRedisCacheService.invalidateAsset(workspaceId, assetId);
        workspaceActivityLogger.logCacheInvalidation(
                com.lebhas.creativesaas.asset.cache.AssetCacheKeys.asset(assetId),
                workspaceId,
                actorUserId);
        long deleted = assetMetadataRedisCacheService.invalidateProjectLists(workspaceId, null);
        if (projectId != null) {
            deleted += assetMetadataRedisCacheService.invalidateProjectLists(workspaceId, projectId);
        }
        if (deleted > 0) {
            workspaceActivityLogger.logCacheInvalidation(
                    com.lebhas.creativesaas.asset.cache.AssetCacheKeys.assetListPattern(workspaceId, projectId),
                    workspaceId,
                    actorUserId);
        }
    }

}
