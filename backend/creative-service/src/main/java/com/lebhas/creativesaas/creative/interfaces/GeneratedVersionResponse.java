package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.generatedversion.domain.ApprovalStatus;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionStatus;
import com.lebhas.creativesaas.generatedversion.domain.GenerationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Generated version resource.")
public record GeneratedVersionResponse(
        UUID id,
        UUID workspaceId,
        UUID brandId,
        UUID productServiceId,
        UUID creativeRequestId,
        UUID projectId,
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
        GeneratedVersionStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
