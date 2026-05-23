package com.lebhas.creativesaas.billing.interfaces;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Pricing plan feature policy response.")
public record PlanFeaturePolicyResponse(
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
        Instant updatedAt
) {
}
