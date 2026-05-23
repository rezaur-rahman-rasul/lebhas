package com.lebhas.creativesaas.storage.application;

import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.domain.AssetType;
import com.lebhas.creativesaas.asset.domain.AssetVariantEntity;
import com.lebhas.creativesaas.asset.infrastructure.persistence.AssetRepository;
import com.lebhas.creativesaas.asset.infrastructure.persistence.AssetVariantRepository;
import com.lebhas.creativesaas.storage.domain.StorageUsageEntity;
import com.lebhas.creativesaas.storage.infrastructure.persistence.StorageUsageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class StorageUsageRecalculationService {

    private final AssetRepository assetRepository;
    private final AssetVariantRepository assetVariantRepository;
    private final StorageUsageRepository storageUsageRepository;
    private final StorageUsageService storageUsageService;

    public StorageUsageRecalculationService(
            AssetRepository assetRepository,
            AssetVariantRepository assetVariantRepository,
            StorageUsageRepository storageUsageRepository,
            StorageUsageService storageUsageService
    ) {
        this.assetRepository = assetRepository;
        this.assetVariantRepository = assetVariantRepository;
        this.storageUsageRepository = storageUsageRepository;
        this.storageUsageService = storageUsageService;
    }

    @Transactional
    public StorageUsageEntity recalculateWorkspaceUsage(UUID workspaceId) {
        List<AssetEntity> assets = assetRepository.findAllByWorkspaceIdAndDeletedFalse(workspaceId);
        List<AssetVariantEntity> variants = assetVariantRepository.findAllByWorkspaceIdAndDeletedFalse(workspaceId);

        long rawAssetBytes = 0L;
        long generatedAssetBytes = 0L;
        for (AssetEntity asset : assets) {
            long fileSize = Math.max(asset.getFileSize(), 0L);
            if (isGeneratedAsset(asset.getAssetType())) {
                generatedAssetBytes += fileSize;
            } else {
                rawAssetBytes += fileSize;
            }
        }

        long variantBytes = variants.stream()
                .mapToLong(variant -> Math.max(variant.getFileSize(), 0L))
                .sum();

        // Existing schema does not retain enough lifecycle detail to derive deleted pending bytes exactly.
        long deletedBytes = storageUsageRepository.findFirstByWorkspaceIdAndDeletedFalse(workspaceId)
                .map(StorageUsageEntity::getDeletedBytes)
                .orElse(0L);

        return storageUsageService.replaceSnapshot(
                workspaceId,
                rawAssetBytes + generatedAssetBytes + variantBytes,
                rawAssetBytes,
                generatedAssetBytes,
                variantBytes,
                deletedBytes,
                Instant.now());
    }

    private boolean isGeneratedAsset(AssetType assetType) {
        return assetType == AssetType.GENERATED
                || assetType == AssetType.GENERATED_CREATIVE
                || assetType == AssetType.EXPORT_IMAGE
                || assetType == AssetType.EXPORT_VIDEO;
    }
}
