package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.asset.application.dto.AssetUrlView;
import com.lebhas.creativesaas.creativerequest.application.dto.CreativeRequestJobView;
import com.lebhas.creativesaas.creativerequest.application.dto.CreativeRequestResponse;
import com.lebhas.creativesaas.creativerequest.application.dto.CreativeRequestView;
import com.lebhas.creativesaas.generatedversion.application.dto.GeneratedVersionView;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Day5ApiMapper {

    public CreativeRequestResourceResponse toCreativeRequestResponse(CreativeRequestResponse response) {
        CreativeRequestView request = response.request();
        return new CreativeRequestResourceResponse(
                request.id(),
                request.workspaceId(),
                request.projectCampaignId(),
                request.requestedBy(),
                request.requestName(),
                request.sourcePrompt(),
                request.enhancedPrompt(),
                request.languagePreference(),
                request.creativeObjective(),
                request.targetPlatform(),
                request.requestedFormat(),
                request.requestedVersions(),
                request.selectedAssetIds(),
                request.status(),
                request.creditReservationId(),
                toGeneratedVersionResponse(response.latestVersion()),
                response.versions().stream().map(this::toGeneratedVersionResponse).toList(),
                toCreativeRequestJobResponse(response.job()),
                response.estimatedCreditCost(),
                request.createdAt(),
                request.updatedAt());
    }

    public GeneratedVersionResponse toGeneratedVersionResponse(GeneratedVersionView view) {
        if (view == null) {
            return null;
        }
        return new GeneratedVersionResponse(
                view.id(),
                view.workspaceId(),
                view.creativeRequestId(),
                view.projectCampaignId(),
                view.versionNumber(),
                view.versionName(),
                view.storageFileId(),
                view.assetId(),
                view.generationStatus(),
                view.approvalStatus(),
                view.editableBeforeApproval(),
                view.generatedByProvider(),
                view.generatedByModel(),
                view.createdByUserId(),
                view.status(),
                view.createdAt(),
                view.updatedAt());
    }

    public List<GeneratedVersionResponse> toGeneratedVersionResponses(List<GeneratedVersionView> views) {
        return views.stream().map(this::toGeneratedVersionResponse).toList();
    }

    public GeneratedVersionPreviewUrlResponse toGeneratedVersionPreviewUrlResponse(
            java.util.UUID generatedVersionId,
            AssetUrlView view
    ) {
        return new GeneratedVersionPreviewUrlResponse(
                generatedVersionId,
                view.url(),
                view.type(),
                view.cdnUrl(),
                view.cached(),
                view.generatedAt(),
                view.expiresAt());
    }

    private CreativeRequestJobResponse toCreativeRequestJobResponse(CreativeRequestJobView view) {
        if (view == null) {
            return null;
        }
        return new CreativeRequestJobResponse(
                view.jobId(),
                view.providerType(),
                view.model(),
                view.state(),
                view.attempt(),
                view.providerJobId(),
                view.message(),
                view.updatedAt());
    }
}
