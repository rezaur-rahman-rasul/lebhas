package com.lebhas.creativesaas.generatedversion.application;

import com.lebhas.creativesaas.generatedversion.application.dto.GeneratedVersionView;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.profile.application.SafeProfileDisplayService;
import com.lebhas.creativesaas.profile.application.dto.SafeProfileDisplayView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GeneratedVersionViewMapper {

    private SafeProfileDisplayService safeProfileDisplayService;

    @Autowired(required = false)
    public void setSafeProfileDisplayService(SafeProfileDisplayService safeProfileDisplayService) {
        this.safeProfileDisplayService = safeProfileDisplayService;
    }

    public GeneratedVersionView toView(GeneratedVersionEntity entity) {
        return new GeneratedVersionView(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getCreativeRequestId(),
                entity.getProjectCampaignId(),
                entity.getVersionNumber(),
                entity.getVersionName(),
                entity.getStorageFileId(),
                entity.getAssetId(),
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

    private SafeProfileDisplayView safeDisplay(java.util.UUID workspaceId, java.util.UUID userId) {
        return safeProfileDisplayService == null ? null : safeProfileDisplayService.forUserInWorkspace(workspaceId, userId);
    }
}
