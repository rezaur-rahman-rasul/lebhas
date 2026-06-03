package com.lebhas.creativesaas.pricing.cache.dto;

import com.lebhas.pricing.PlanFeaturePolicy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record PlanFeaturePolicyCacheEntry(
        UUID id,
        UUID pricingPlanId,
        Integer maxGeneratedVersionsPerRequest,
        Integer maxBrands,
        Integer maxProductServices,
        Integer maxProjects,
        Integer maxAssets,
        Integer maxCreativeRequests,
        Integer maxTeamMembers,
        Integer maxGeneratedVersionsPerCreativeRequest,
        BigDecimal maxStorageGb,
        Long maxStorageBytes,
        BigDecimal monthlyCreditLimit,
        boolean promptEnhancementEnabled,
        boolean creativeGenerationEnabled,
        boolean allowApprovalWorkflow,
        boolean downloadEnabled,
        boolean shareEnabled,
        boolean allowPublicShareLinks,
        boolean assetUploadEnabled,
        boolean premiumQualityEnabled,
        boolean allowVideoGeneration,
        boolean voiceoverGenerationEnabled,
        boolean allowAdvancedPromptIntelligence,
        boolean allowTeamCollaboration,
        boolean allowExportWithoutWatermark,
        Set<String> enabledCreativeToolCodes,
        Instant createdAt,
        Instant updatedAt,
        Instant cachedAt
) {

    public static PlanFeaturePolicyCacheEntry from(PlanFeaturePolicy policy) {
        return new PlanFeaturePolicyCacheEntry(
                policy.getId(),
                policy.getPricingPlanId(),
                policy.getMaxGeneratedVersionsPerRequest(),
                policy.getMaxBrands(),
                policy.getMaxProductServices(),
                policy.getMaxProjects(),
                policy.getMaxAssets(),
                policy.getMaxCreativeRequests(),
                policy.getMaxTeamMembers(),
                policy.getMaxGeneratedVersionsPerCreativeRequest(),
                policy.getMaxStorageGb(),
                policy.getMaxStorageBytes(),
                policy.getMonthlyCreditLimit(),
                policy.isPromptEnhancementEnabled(),
                policy.isCreativeGenerationEnabled(),
                policy.isAllowApprovalWorkflow(),
                policy.isDownloadEnabled(),
                policy.isShareEnabled(),
                policy.isAllowPublicShareLinks(),
                policy.isAssetUploadEnabled(),
                policy.isPremiumQualityEnabled(),
                policy.isAllowVideoGeneration(),
                policy.isVoiceoverGenerationEnabled(),
                policy.isAllowAdvancedPromptIntelligence(),
                policy.isAllowTeamCollaboration(),
                policy.isAllowExportWithoutWatermark(),
                policy.getEnabledCreativeToolCodes(),
                policy.getCreatedAt(),
                policy.getUpdatedAt(),
                Instant.now());
    }
}
