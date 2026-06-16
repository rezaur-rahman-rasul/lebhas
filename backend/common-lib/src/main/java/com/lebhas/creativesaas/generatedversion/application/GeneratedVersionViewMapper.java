package com.lebhas.creativesaas.generatedversion.application;

import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.infrastructure.persistence.AssetRepository;
import com.lebhas.creativesaas.asset.storage.StorageService;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
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
    private StorageService storageService;

    @Autowired(required = false)
    public void setSafeProfileDisplayService(SafeProfileDisplayService safeProfileDisplayService) {
        this.safeProfileDisplayService = safeProfileDisplayService;
    }

    @Autowired(required = false)
    public void setAssetRepository(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    @Autowired(required = false)
    public void setStorageService(StorageService storageService) {
        this.storageService = storageService;
    }

    public GeneratedVersionView toView(GeneratedVersionEntity entity) {
        AssetEntity generatedAsset = generatedAsset(entity);
        CreativeRequestEntity creativeRequest = creativeRequest(entity);
        String signedPreviewUrl = signedPreviewUrl(generatedAsset);
        String signedDownloadUrl = signedDownloadUrl(generatedAsset);
        return new GeneratedVersionView(
                entity.getId(),
                entity.getWorkspaceId(),
                creativeRequest == null ? null : creativeRequest.getBrandId(),
                creativeRequest == null ? null : creativeRequest.getProductServiceId(),
                entity.getCreativeRequestId(),
                entity.getProjectCampaignId(),
                creativeRequest == null ? null : creativeRequest.getTargetPlatform(),
                creativeRequest == null || creativeRequest.getCreativeType() == null
                        ? null
                        : creativeRequest.getCreativeType().name(),
                creativeRequest == null || creativeRequest.getLanguagePreference() == null
                        ? null
                        : creativeRequest.getLanguagePreference().name(),
                entity.getVersionNumber(),
                entity.getVersionName(),
                entity.getStorageFileId(),
                entity.getAssetId(),
                entity.getGeneratedAssetId(),
                generatedAsset == null ? null : generatedAsset.getStorageKey(),
                firstNonBlank(generatedAsset == null ? null : generatedAsset.getPreviewUrl(), signedPreviewUrl),
                signedPreviewUrl,
                generatedAsset == null ? null : generatedAsset.getThumbnailUrl(),
                firstNonBlank(generatedAsset == null ? null : generatedAsset.getPublicUrl(), signedDownloadUrl),
                signedDownloadUrl,
                generatedAsset == null ? null : generatedAsset.getFileSize(),
                entity.getWidth() == null && generatedAsset != null ? generatedAsset.getWidth() : entity.getWidth(),
                entity.getHeight() == null && generatedAsset != null ? generatedAsset.getHeight() : entity.getHeight(),
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

    private CreativeRequestEntity creativeRequest(GeneratedVersionEntity entity) {
        try {
            return entity.getCreativeRequest();
        } catch (RuntimeException ignored) {
            return null;
        }
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

    private String signedPreviewUrl(AssetEntity asset) {
        if (asset == null || storageService == null) {
            return null;
        }
        try {
            return storageService.generatePreviewUrl(asset).url();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String signedDownloadUrl(AssetEntity asset) {
        if (asset == null || storageService == null) {
            return null;
        }
        try {
            return storageService.generateDownloadUrl(asset).url();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null || second.isBlank() ? null : second;
    }

    private SafeProfileDisplayView safeDisplay(java.util.UUID workspaceId, java.util.UUID userId) {
        return safeProfileDisplayService == null ? null : safeProfileDisplayService.forUserInWorkspace(workspaceId, userId);
    }
}
