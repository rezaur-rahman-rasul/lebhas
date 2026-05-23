package com.lebhas.creativesaas.generatedversion.application;

import com.lebhas.creativesaas.generatedversion.application.dto.GeneratedVersionView;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import org.springframework.stereotype.Component;

@Component
public class GeneratedVersionViewMapper {

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
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
