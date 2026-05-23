package com.lebhas.creativesaas.pricing.cache.dto;

import com.lebhas.pricing.PlanFeaturePolicy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PlanFeaturePolicyCacheEntry(
        UUID id,
        UUID pricingPlanId,
        Integer maxGeneratedVersionsPerRequest,
        Integer maxBrands,
        Integer maxProductServices,
        Integer maxProjects,
        Integer maxTeamMembers,
        BigDecimal maxStorageGb,
        BigDecimal monthlyCreditLimit,
        boolean allowApprovalWorkflow,
        boolean allowPublicShareLinks,
        boolean allowVideoGeneration,
        boolean allowAdvancedPromptIntelligence,
        boolean allowTeamCollaboration,
        boolean allowExportWithoutWatermark,
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
                policy.getMaxTeamMembers(),
                policy.getMaxStorageGb(),
                policy.getMonthlyCreditLimit(),
                policy.isAllowApprovalWorkflow(),
                policy.isAllowPublicShareLinks(),
                policy.isAllowVideoGeneration(),
                policy.isAllowAdvancedPromptIntelligence(),
                policy.isAllowTeamCollaboration(),
                policy.isAllowExportWithoutWatermark(),
                policy.getCreatedAt(),
                policy.getUpdatedAt(),
                Instant.now());
    }
}
