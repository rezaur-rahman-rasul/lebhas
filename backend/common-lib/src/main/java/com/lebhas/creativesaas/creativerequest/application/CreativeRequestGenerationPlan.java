package com.lebhas.creativesaas.creativerequest.application;

import com.lebhas.ai.dto.AiGenerationRequest;
import com.lebhas.creativesaas.brand.domain.BrandLanguagePreference;
import com.lebhas.creativesaas.generation.domain.CreativeOutputFormat;
import com.lebhas.creativesaas.generation.domain.CreativeType;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

record CreativeRequestGenerationPlan(
        UUID activePipelineId,
        UUID brandId,
        UUID productServiceId,
        UUID projectCampaignId,
        String requestName,
        String sourcePrompt,
        String enhancedPrompt,
        BrandLanguagePreference languagePreference,
        String campaignTone,
        String targetAudience,
        String ctaPreference,
        String creativeObjectiveText,
        String targetPlatformText,
        String requestedFormatText,
        int requestedVersions,
        List<UUID> selectedAssetIds,
        PromptPlatform platform,
        CampaignObjective creativeObjective,
        CreativeOutputFormat outputFormat,
        CreativeType creativeType,
        PromptLanguage promptLanguage,
        BigDecimal estimatedCreditCost,
        String duplicateRequestHash
) {

    AiGenerationRequest toAiGenerationRequest(UUID workspaceId, UUID creativeRequestId, UUID jobId) {
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("activePipelineId", activePipelineId);
        generationConfig.put("brandId", brandId);
        generationConfig.put("productServiceId", productServiceId);
        generationConfig.put("projectCampaignId", projectCampaignId);
        generationConfig.put("requestedVersions", requestedVersions);
        generationConfig.put("selectedAssetIds", selectedAssetIds);
        generationConfig.put("requestName", requestName);
        return new AiGenerationRequest(
                workspaceId,
                creativeRequestId,
                jobId,
                creativeType,
                platform,
                creativeObjective,
                outputFormat,
                promptLanguage,
                providerPrompt(),
                null,
                assetContextSnapshot(),
                generationConfig,
                null,
                null,
                null);
    }

    String providerPrompt() {
        if (enhancedPrompt != null && !enhancedPrompt.isBlank()) {
            return enhancedPrompt;
        }
        return sourcePrompt;
    }

    String assetContextSnapshot() {
        if (selectedAssetIds.isEmpty()) {
            return null;
        }
        return selectedAssetIds.stream()
                .map(UUID::toString)
                .reduce((left, right) -> left + "," + right)
                .orElse(null);
    }
}
