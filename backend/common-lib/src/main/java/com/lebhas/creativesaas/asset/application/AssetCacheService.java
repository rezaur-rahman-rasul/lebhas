package com.lebhas.creativesaas.asset.application;

import com.lebhas.creativesaas.asset.application.dto.AssetListCriteria;
import com.lebhas.creativesaas.asset.application.dto.AssetView;
import com.lebhas.creativesaas.asset.cache.AssetMetadataRedisCacheService;
import com.lebhas.creativesaas.common.api.PagedResult;
import com.lebhas.creativesaas.workspace.application.WorkspaceActivityLogger;
import org.springframework.stereotype.Service;

import java.util.Objects;
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
        String signature = signature(criteria);
        var cached = assetMetadataRedisCacheService.getProjectList(criteria.workspaceId(), criteria.projectId(), criteria.page()).orElse(null);
        if (cached != null && Objects.equals(cached.criteriaSignature(), signature)) {
            return cached.result();
        }
        PagedResult<AssetView> loaded = loader.get();
        assetMetadataRedisCacheService.cacheProjectList(
                criteria.workspaceId(),
                criteria.projectId(),
                criteria.page(),
                signature,
                loaded);
        return loaded;
    }

    public void invalidate(UUID workspaceId, UUID projectId, UUID assetId, UUID actorUserId) {
        assetMetadataRedisCacheService.invalidateAsset(workspaceId, assetId);
        workspaceActivityLogger.logCacheInvalidation(
                com.lebhas.creativesaas.asset.cache.AssetCacheKeys.asset(assetId),
                workspaceId,
                actorUserId);
        long deleted = assetMetadataRedisCacheService.invalidateProjectLists(workspaceId, projectId);
        if (deleted > 0) {
            workspaceActivityLogger.logCacheInvalidation(
                    com.lebhas.creativesaas.asset.cache.AssetCacheKeys.assetListProjectPattern(projectId),
                    workspaceId,
                    actorUserId);
        }
    }

    private String signature(AssetListCriteria criteria) {
        return String.join("|",
                String.valueOf(criteria.assetType()),
                String.valueOf(criteria.assetCategory()),
                String.valueOf(criteria.previewStatus()),
                String.valueOf(criteria.processingStatus()),
                String.valueOf(criteria.uploadedBy()),
                String.valueOf(criteria.status()),
                String.valueOf(criteria.keyword()),
                String.valueOf(criteria.createdFrom()),
                String.valueOf(criteria.createdTo()),
                String.valueOf(criteria.page()),
                String.valueOf(criteria.size()),
                String.valueOf(criteria.sortBy()),
                String.valueOf(criteria.sortDirection()));
    }

}
