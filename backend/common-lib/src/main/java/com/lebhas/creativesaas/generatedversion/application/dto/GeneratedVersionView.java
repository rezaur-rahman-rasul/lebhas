package com.lebhas.creativesaas.generatedversion.application.dto;

import com.lebhas.creativesaas.generatedversion.domain.ApprovalStatus;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionStatus;
import com.lebhas.creativesaas.generatedversion.domain.GenerationStatus;
import com.lebhas.creativesaas.profile.application.dto.SafeProfileDisplayView;

import java.time.Instant;
import java.util.UUID;

public record GeneratedVersionView(
        UUID id,
        UUID workspaceId,
        UUID brandId,
        UUID productServiceId,
        UUID creativeRequestId,
        UUID projectCampaignId,
        String platform,
        String creativeType,
        String language,
        int versionNumber,
        String versionName,
        UUID storageFileId,
        UUID assetId,
        UUID generatedAssetId,
        String r2ObjectKey,
        String previewUrl,
        String signedPreviewUrl,
        String thumbnailUrl,
        String downloadUrl,
        String signedDownloadUrl,
        Long fileSize,
        Integer width,
        Integer height,
        GenerationStatus generationStatus,
        ApprovalStatus approvalStatus,
        boolean editableBeforeApproval,
        String generatedByProvider,
        String generatedByModel,
        UUID createdByUserId,
        SafeProfileDisplayView createdByDisplay,
        GeneratedVersionStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
