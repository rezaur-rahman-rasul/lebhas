package com.lebhas.creativesaas.asset.cache;

import com.lebhas.creativesaas.asset.application.dto.AssetView;
import com.lebhas.creativesaas.asset.cache.dto.AssetListCacheEntry;
import com.lebhas.creativesaas.asset.cache.dto.AssetMetadataCacheEntry;
import com.lebhas.creativesaas.common.api.PagedResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AssetMetadataRedisCacheService {

    private final AssetRedisAccessSupport redisAccessSupport;
    private final AssetCacheTtlStrategy ttlStrategy;

    public AssetMetadataRedisCacheService(
            AssetRedisAccessSupport redisAccessSupport,
            AssetCacheTtlStrategy ttlStrategy
    ) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<AssetMetadataCacheEntry> getAsset(UUID workspaceId, UUID assetId) {
        return redisAccessSupport.read(AssetCacheKeys.asset(assetId), AssetMetadataCacheEntry.class, workspaceId, assetId);
    }

    public void cacheAsset(AssetView assetView) {
        redisAccessSupport.write(
                AssetCacheKeys.asset(assetView.id()),
                new AssetMetadataCacheEntry(
                        assetView.id(),
                        assetView.workspaceId(),
                        assetView.projectCampaignId(),
                        assetView,
                        Instant.now()),
                ttlStrategy.assetMetadataTtl(),
                assetView.workspaceId(),
                assetView.id());
    }

    public Optional<AssetListCacheEntry> getProjectList(UUID workspaceId, UUID projectId, int page) {
        return redisAccessSupport.read(
                AssetCacheKeys.assetList(workspaceId, projectId, page),
                AssetListCacheEntry.class,
                workspaceId,
                null);
    }

    public void cacheProjectList(
            UUID workspaceId,
            UUID projectId,
            int page,
            String criteriaSignature,
            PagedResult<AssetView> result
    ) {
        redisAccessSupport.write(
                AssetCacheKeys.assetList(workspaceId, projectId, page),
                new AssetListCacheEntry(
                        workspaceId,
                        projectId,
                        page,
                        criteriaSignature,
                        result,
                        Instant.now()),
                ttlStrategy.assetListTtl(),
                workspaceId,
                null);
    }

    public void invalidateAsset(UUID workspaceId, UUID assetId) {
        redisAccessSupport.delete(AssetCacheKeys.asset(assetId), workspaceId, assetId);
    }

    public long invalidateProjectLists(UUID workspaceId, UUID projectId) {
        return redisAccessSupport.deleteByPattern(AssetCacheKeys.assetListPattern(workspaceId, projectId), workspaceId, null);
    }
}
