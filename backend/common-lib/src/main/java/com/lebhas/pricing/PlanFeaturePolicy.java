package com.lebhas.pricing;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Entity
@Table(name = "plan_feature_policies", schema = "platform")
public class PlanFeaturePolicy extends BaseEntity {

    @Column(name = "pricing_plan_id", nullable = false)
    private UUID pricingPlanId;

    @Column(name = "max_generated_versions_per_request")
    private Integer maxGeneratedVersionsPerRequest;

    @Column(name = "max_brands")
    private Integer maxBrands;

    @Column(name = "max_product_services")
    private Integer maxProductServices;

    @Column(name = "max_projects")
    private Integer maxProjects;

    @Column(name = "max_team_members")
    private Integer maxTeamMembers;

    @Column(name = "max_storage_gb", precision = 19, scale = 4)
    private BigDecimal maxStorageGb;

    @Column(name = "monthly_credit_limit", precision = 19, scale = 4)
    private BigDecimal monthlyCreditLimit;

    @Column(name = "allow_approval_workflow", nullable = false)
    private boolean allowApprovalWorkflow;

    @Column(name = "allow_public_share_links", nullable = false)
    private boolean allowPublicShareLinks;

    @Column(name = "allow_video_generation", nullable = false)
    private boolean allowVideoGeneration;

    @Column(name = "allow_advanced_prompt_intelligence", nullable = false)
    private boolean allowAdvancedPromptIntelligence;

    @Column(name = "allow_team_collaboration", nullable = false)
    private boolean allowTeamCollaboration;

    @Column(name = "allow_export_without_watermark", nullable = false)
    private boolean allowExportWithoutWatermark;

    protected PlanFeaturePolicy() {
    }

    public static PlanFeaturePolicy create(
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
            boolean allowExportWithoutWatermark
    ) {
        PlanFeaturePolicy policy = new PlanFeaturePolicy();
        policy.pricingPlanId = requirePricingPlanId(pricingPlanId);
        policy.maxGeneratedVersionsPerRequest = normalizeLimit(maxGeneratedVersionsPerRequest, "maxGeneratedVersionsPerRequest");
        policy.maxBrands = normalizeLimit(maxBrands, "maxBrands");
        policy.maxProductServices = normalizeLimit(maxProductServices, "maxProductServices");
        policy.maxProjects = normalizeLimit(maxProjects, "maxProjects");
        policy.maxTeamMembers = normalizeLimit(maxTeamMembers, "maxTeamMembers");
        policy.maxStorageGb = normalizeDecimalLimit(maxStorageGb, "maxStorageGb");
        policy.monthlyCreditLimit = normalizeDecimalLimit(monthlyCreditLimit, "monthlyCreditLimit");
        policy.allowApprovalWorkflow = allowApprovalWorkflow;
        policy.allowPublicShareLinks = allowPublicShareLinks;
        policy.allowVideoGeneration = allowVideoGeneration;
        policy.allowAdvancedPromptIntelligence = allowAdvancedPromptIntelligence;
        policy.allowTeamCollaboration = allowTeamCollaboration;
        policy.allowExportWithoutWatermark = allowExportWithoutWatermark;
        return policy;
    }

    public UUID getPricingPlanId() {
        return pricingPlanId;
    }

    public Integer getMaxGeneratedVersionsPerRequest() {
        return maxGeneratedVersionsPerRequest;
    }

    public Integer getMaxBrands() {
        return maxBrands;
    }

    public Integer getMaxProductServices() {
        return maxProductServices;
    }

    public Integer getMaxProjects() {
        return maxProjects;
    }

    public Integer getMaxTeamMembers() {
        return maxTeamMembers;
    }

    public BigDecimal getMaxStorageGb() {
        return maxStorageGb;
    }

    public BigDecimal getMonthlyCreditLimit() {
        return monthlyCreditLimit;
    }

    public boolean isAllowApprovalWorkflow() {
        return allowApprovalWorkflow;
    }

    public boolean isAllowPublicShareLinks() {
        return allowPublicShareLinks;
    }

    public boolean isAllowVideoGeneration() {
        return allowVideoGeneration;
    }

    public boolean isAllowAdvancedPromptIntelligence() {
        return allowAdvancedPromptIntelligence;
    }

    public boolean isAllowTeamCollaboration() {
        return allowTeamCollaboration;
    }

    public boolean isAllowExportWithoutWatermark() {
        return allowExportWithoutWatermark;
    }

    public void update(
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
            boolean allowExportWithoutWatermark
    ) {
        this.maxGeneratedVersionsPerRequest = normalizeLimit(maxGeneratedVersionsPerRequest, "maxGeneratedVersionsPerRequest");
        this.maxBrands = normalizeLimit(maxBrands, "maxBrands");
        this.maxProductServices = normalizeLimit(maxProductServices, "maxProductServices");
        this.maxProjects = normalizeLimit(maxProjects, "maxProjects");
        this.maxTeamMembers = normalizeLimit(maxTeamMembers, "maxTeamMembers");
        this.maxStorageGb = normalizeDecimalLimit(maxStorageGb, "maxStorageGb");
        this.monthlyCreditLimit = normalizeDecimalLimit(monthlyCreditLimit, "monthlyCreditLimit");
        this.allowApprovalWorkflow = allowApprovalWorkflow;
        this.allowPublicShareLinks = allowPublicShareLinks;
        this.allowVideoGeneration = allowVideoGeneration;
        this.allowAdvancedPromptIntelligence = allowAdvancedPromptIntelligence;
        this.allowTeamCollaboration = allowTeamCollaboration;
        this.allowExportWithoutWatermark = allowExportWithoutWatermark;
    }

    private static UUID requirePricingPlanId(UUID pricingPlanId) {
        if (pricingPlanId == null) {
            throw new IllegalArgumentException("pricingPlanId must not be null");
        }
        return pricingPlanId;
    }

    private static Integer normalizeLimit(Integer value, String field) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        return value;
    }

    private static BigDecimal normalizeDecimalLimit(BigDecimal value, String field) {
        if (value == null) {
            return null;
        }
        BigDecimal normalized = value.setScale(4, RoundingMode.HALF_UP);
        if (normalized.signum() < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        return normalized;
    }
}
