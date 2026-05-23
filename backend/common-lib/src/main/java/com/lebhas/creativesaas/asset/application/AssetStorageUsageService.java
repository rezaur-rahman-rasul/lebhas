package com.lebhas.creativesaas.asset.application;

import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.domain.AssetType;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.storage.application.StorageUsageService;
import com.lebhas.creativesaas.storage.domain.StorageUsageEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AssetStorageUsageService {

    private final StorageUsageService storageUsageService;

    public AssetStorageUsageService(
            StorageUsageService storageUsageService
    ) {
        this.storageUsageService = storageUsageService;
    }

    @Transactional
    public StorageUsageEntity getOrCreateSnapshot(UUID workspaceId) {
        return storageUsageService.getOrCreateSnapshot(workspaceId);
    }

    @Transactional(readOnly = true)
    public void validateWorkspaceQuota(UUID workspaceId, long incomingBytes, AssetServiceProperties properties) {
        if (!properties.isWorkspaceStorageLimited()) {
            return;
        }
        long currentBytesUsed = storageUsageService.getTotalUsedBytes(workspaceId);
        if (currentBytesUsed + Math.max(incomingBytes, 0L) > properties.getMaxWorkspaceStorageBytes()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Workspace storage quota would be exceeded");
        }
    }

    @Transactional
    public StorageUsageEntity recordUpload(UUID workspaceId, AssetType assetType, long addedBytes) {
        return storageUsageService.recordUpload(workspaceId, assetType, addedBytes);
    }

    @Transactional
    public StorageUsageEntity recordUpload(AssetEntity asset, long addedBytes, Long storageLimitBytes, String reason) {
        return storageUsageService.recordUpload(
                asset.getWorkspaceId(),
                asset.getId(),
                asset.getAssetType(),
                addedBytes,
                storageLimitBytes,
                reason);
    }

    @Transactional
    public StorageUsageEntity recordSoftDelete(AssetEntity asset, boolean storageReleased) {
        return storageUsageService.recordSoftDelete(asset, storageReleased);
    }

    @Transactional
    public void invalidateWorkspaceSummary(UUID workspaceId) {
        storageUsageService.invalidateCache(workspaceId);
    }
}
