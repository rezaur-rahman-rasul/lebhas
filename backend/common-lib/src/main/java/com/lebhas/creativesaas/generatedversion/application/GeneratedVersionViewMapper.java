package com.lebhas.creativesaas.generatedversion.application;

import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.infrastructure.persistence.AssetRepository;
import com.lebhas.creativesaas.generatedversion.application.dto.GeneratedVersionView;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.profile.application.SafeProfileDisplayService;
import com.lebhas.creativesaas.profile.application.dto.SafeProfileDisplayView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GeneratedVersionViewMapper {

    private SafeProfileDisplayService safeProfileDisplayService;
    private AssetRepository assetRepository;

    @Autowired(required = false)
    public void setSafeProfileDisplayService(SafeProfileDisplayService safeProfileDisplayService) {
        this.safeProfileDisplayService = safeProfileDisplayService;
    }

    @Autowired(required = false)
    public void setAssetRepository(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    public GeneratedVersionView toView(GeneratedVersionEntity entity) {
        AssetEntity generatedAsset = generatedAsset(entity);
        return new GeneratedVersionView(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getCreativeRequestId(),
                entity.getProjectCampaignId(),
                entity.getVersionNumber(),
                entity.getVersionName(),
                entity.getStorageFileId(),
                entity.getAssetId(),
                generatedAsset == null ? null : generatedAsset.getPreviewUrl(),
                generatedAsset == null ? null : generatedAsset.getThumbnailUrl(),
                entity.getGenerationStatus(),
                entity.getApprovalStatus(),
                entity.isEditableBeforeApproval(),
                entity.getGeneratedByProvider(),
                entity.getGeneratedByModel(),
                entity.getCreatedByUserId(),
                safeDisplay(entity.getWorkspaceId(), entity.getCreatedByUserId()),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private AssetEntity generatedAsset(GeneratedVersionEntity entity) {
        if (entity.getGeneratedAsset() != null) {
            return entity.getGeneratedAsset();
        }
        if (assetRepository == null || entity.getAssetId() == null) {
            return null;
        }
        return assetRepository.findByIdAndWorkspaceIdAndDeletedFalse(entity.getAssetId(), entity.getWorkspaceId())
                .orElse(null);
    }

    private SafeProfileDisplayView safeDisplay(java.util.UUID workspaceId, java.util.UUID userId) {
        return safeProfileDisplayService == null ? null : safeProfileDisplayService.forUserInWorkspace(workspaceId, userId);
    }
}
