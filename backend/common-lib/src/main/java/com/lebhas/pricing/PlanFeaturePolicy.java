package com.lebhas.pricing;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.Set;
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

    @Column(name = "max_assets")
    private Integer maxAssets;

    @Column(name = "max_creative_requests")
    private Integer maxCreativeRequests;

    @Column(name = "max_team_members")
    private Integer maxTeamMembers;

    @Column(name = "max_generated_versions_per_creative_request")
    private Integer maxGeneratedVersionsPerCreativeRequest;

    @Column(name = "max_storage_gb", precision = 19, scale = 4)
    private BigDecimal maxStorageGb;

    @Column(name = "max_storage_bytes")
    private Long maxStorageBytes;

    @Column(name = "monthly_credit_limit", precision = 19, scale = 4)
    private BigDecimal monthlyCreditLimit;

    @Column(name = "prompt_enhancement_enabled", nullable = false)
    private boolean promptEnhancementEnabled;

    @Column(name = "creative_generation_enabled", nullable = false)
    private boolean creativeGenerationEnabled;

    @Column(name = "allow_approval_workflow", nullable = false)
    private boolean allowApprovalWorkflow;

    @Column(name = "download_enabled", nullable = false)
    private boolean downloadEnabled;

    @Column(name = "share_enabled", nullable = false)
    private boolean shareEnabled;

    @Column(name = "allow_public_share_links", nullable = false)
    private boolean allowPublicShareLinks;

    @Column(name = "asset_upload_enabled", nullable = false)
    private boolean assetUploadEnabled;

    @Column(name = "premium_quality_enabled", nullable = false)
    private boolean premiumQualityEnabled;

    @Column(name = "allow_video_generation", nullable = false)
    private boolean allowVideoGeneration;

    @Column(name = "voiceover_generation_enabled", nullable = false)
    private boolean voiceoverGenerationEnabled;

    @Column(name = "allow_advanced_prompt_intelligence", nullable = false)
    private boolean allowAdvancedPromptIntelligence;

    @Column(name = "allow_team_collaboration", nullable = false)
    private boolean allowTeamCollaboration;

    @Column(name = "allow_export_without_watermark", nullable = false)
    private boolean allowExportWithoutWatermark;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "enabled_creative_tool_codes", nullable = false, columnDefinition = "jsonb")
    private Set<String> enabledCreativeToolCodes = new LinkedHashSet<>();

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
        return create(
                pricingPlanId,
                maxGeneratedVersionsPerRequest,
                maxBrands,
                maxProductServices,
                maxProjects,
                null,
                null,
                maxTeamMembers,
                maxGeneratedVersionsPerRequest,
                maxStorageGb,
                null,
                monthlyCreditLimit,
                allowAdvancedPromptIntelligence,
                true,
                allowApprovalWorkflow,
                true,
                allowPublicShareLinks,
                allowPublicShareLinks,
                true,
                false,
                allowVideoGeneration,
                false,
                        allowAdvancedPromptIntelligence,
                        allowTeamCollaboration,
                        allowExportWithoutWatermark,
                        Set.of());
    }

    public static PlanFeaturePolicy create(
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
            boolean allowExportWithoutWatermark
    ) {
        return create(
                pricingPlanId,
                maxGeneratedVersionsPerRequest,
                maxBrands,
                maxProductServices,
                maxProjects,
                maxAssets,
                maxCreativeRequests,
                maxTeamMembers,
                maxGeneratedVersionsPerCreativeRequest,
                maxStorageGb,
                maxStorageBytes,
                monthlyCreditLimit,
                promptEnhancementEnabled,
                creativeGenerationEnabled,
                allowApprovalWorkflow,
                downloadEnabled,
                shareEnabled,
                allowPublicShareLinks,
                assetUploadEnabled,
                premiumQualityEnabled,
                allowVideoGeneration,
                voiceoverGenerationEnabled,
                allowAdvancedPromptIntelligence,
                allowTeamCollaboration,
                allowExportWithoutWatermark,
                Set.of());
    }

    public static PlanFeaturePolicy create(
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
            Set<String> enabledCreativeToolCodes
    ) {
        PlanFeaturePolicy policy = new PlanFeaturePolicy();
        policy.pricingPlanId = requirePricingPlanId(pricingPlanId);
        policy.maxGeneratedVersionsPerRequest = normalizeLimit(maxGeneratedVersionsPerRequest, "maxGeneratedVersionsPerRequest");
        policy.maxBrands = normalizeLimit(maxBrands, "maxBrands");
        policy.maxProductServices = normalizeLimit(maxProductServices, "maxProductServices");
        policy.maxProjects = normalizeLimit(maxProjects, "maxProjects");
        policy.maxAssets = normalizeLimit(maxAssets, "maxAssets");
        policy.maxCreativeRequests = normalizeLimit(maxCreativeRequests, "maxCreativeRequests");
        policy.maxTeamMembers = normalizeLimit(maxTeamMembers, "maxTeamMembers");
        policy.maxGeneratedVersionsPerCreativeRequest = normalizeLimit(maxGeneratedVersionsPerCreativeRequest, "maxGeneratedVersionsPerCreativeRequest");
        policy.maxStorageGb = normalizeDecimalLimit(maxStorageGb, "maxStorageGb");
        policy.maxStorageBytes = normalizeLongLimit(maxStorageBytes, "maxStorageBytes");
        policy.monthlyCreditLimit = normalizeDecimalLimit(monthlyCreditLimit, "monthlyCreditLimit");
        policy.promptEnhancementEnabled = promptEnhancementEnabled;
        policy.creativeGenerationEnabled = creativeGenerationEnabled;
        policy.allowApprovalWorkflow = allowApprovalWorkflow;
        policy.downloadEnabled = downloadEnabled;
        policy.shareEnabled = shareEnabled;
        policy.allowPublicShareLinks = allowPublicShareLinks;
        policy.assetUploadEnabled = assetUploadEnabled;
        policy.premiumQualityEnabled = premiumQualityEnabled;
        policy.allowVideoGeneration = allowVideoGeneration;
        policy.voiceoverGenerationEnabled = voiceoverGenerationEnabled;
        policy.allowAdvancedPromptIntelligence = allowAdvancedPromptIntelligence;
        policy.allowTeamCollaboration = allowTeamCollaboration;
        policy.allowExportWithoutWatermark = allowExportWithoutWatermark;
        policy.enabledCreativeToolCodes = normalizeToolCodes(enabledCreativeToolCodes);
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

    public Integer getMaxAssets() {
        return maxAssets;
    }

    public Integer getMaxCreativeRequests() {
        return maxCreativeRequests;
    }

    public Integer getMaxTeamMembers() {
        return maxTeamMembers;
    }

    public Integer getMaxGeneratedVersionsPerCreativeRequest() {
        return maxGeneratedVersionsPerCreativeRequest;
    }

    public BigDecimal getMaxStorageGb() {
        return maxStorageGb;
    }

    public Long getMaxStorageBytes() {
        return maxStorageBytes;
    }

    public BigDecimal getMonthlyCreditLimit() {
        return monthlyCreditLimit;
    }

    public boolean isPromptEnhancementEnabled() {
        return promptEnhancementEnabled;
    }

    public boolean isCreativeGenerationEnabled() {
        return creativeGenerationEnabled;
    }

    public boolean isAllowApprovalWorkflow() {
        return allowApprovalWorkflow;
    }

    public boolean isDownloadEnabled() {
        return downloadEnabled;
    }

    public boolean isShareEnabled() {
        return shareEnabled;
    }

    public boolean isAllowPublicShareLinks() {
        return allowPublicShareLinks;
    }

    public boolean isAssetUploadEnabled() {
        return assetUploadEnabled;
    }

    public boolean isPremiumQualityEnabled() {
        return premiumQualityEnabled;
    }

    public boolean isAllowVideoGeneration() {
        return allowVideoGeneration;
    }

    public boolean isVoiceoverGenerationEnabled() {
        return voiceoverGenerationEnabled;
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

    public Set<String> getEnabledCreativeToolCodes() {
        return Set.copyOf(enabledCreativeToolCodes);
    }

    public void update(
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
            boolean allowExportWithoutWatermark
    ) {
        update(
                maxGeneratedVersionsPerRequest,
                maxBrands,
                maxProductServices,
                maxProjects,
                maxAssets,
                maxCreativeRequests,
                maxTeamMembers,
                maxGeneratedVersionsPerCreativeRequest,
                maxStorageGb,
                maxStorageBytes,
                monthlyCreditLimit,
                promptEnhancementEnabled,
                creativeGenerationEnabled,
                allowApprovalWorkflow,
                downloadEnabled,
                shareEnabled,
                allowPublicShareLinks,
                assetUploadEnabled,
                premiumQualityEnabled,
                allowVideoGeneration,
                voiceoverGenerationEnabled,
                allowAdvancedPromptIntelligence,
                allowTeamCollaboration,
                allowExportWithoutWatermark,
                this.enabledCreativeToolCodes);
    }

    public void update(
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
            Set<String> enabledCreativeToolCodes
    ) {
        this.maxGeneratedVersionsPerRequest = normalizeLimit(maxGeneratedVersionsPerRequest, "maxGeneratedVersionsPerRequest");
        this.maxBrands = normalizeLimit(maxBrands, "maxBrands");
        this.maxProductServices = normalizeLimit(maxProductServices, "maxProductServices");
        this.maxProjects = normalizeLimit(maxProjects, "maxProjects");
        this.maxAssets = normalizeLimit(maxAssets, "maxAssets");
        this.maxCreativeRequests = normalizeLimit(maxCreativeRequests, "maxCreativeRequests");
        this.maxTeamMembers = normalizeLimit(maxTeamMembers, "maxTeamMembers");
        this.maxGeneratedVersionsPerCreativeRequest = normalizeLimit(maxGeneratedVersionsPerCreativeRequest, "maxGeneratedVersionsPerCreativeRequest");
        this.maxStorageGb = normalizeDecimalLimit(maxStorageGb, "maxStorageGb");
        this.maxStorageBytes = normalizeLongLimit(maxStorageBytes, "maxStorageBytes");
        this.monthlyCreditLimit = normalizeDecimalLimit(monthlyCreditLimit, "monthlyCreditLimit");
        this.promptEnhancementEnabled = promptEnhancementEnabled;
        this.creativeGenerationEnabled = creativeGenerationEnabled;
        this.allowApprovalWorkflow = allowApprovalWorkflow;
        this.downloadEnabled = downloadEnabled;
        this.shareEnabled = shareEnabled;
        this.allowPublicShareLinks = allowPublicShareLinks;
        this.assetUploadEnabled = assetUploadEnabled;
        this.premiumQualityEnabled = premiumQualityEnabled;
        this.allowVideoGeneration = allowVideoGeneration;
        this.voiceoverGenerationEnabled = voiceoverGenerationEnabled;
        this.allowAdvancedPromptIntelligence = allowAdvancedPromptIntelligence;
        this.allowTeamCollaboration = allowTeamCollaboration;
        this.allowExportWithoutWatermark = allowExportWithoutWatermark;
        this.enabledCreativeToolCodes = normalizeToolCodes(enabledCreativeToolCodes);
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

    private static Long normalizeLongLimit(Long value, String field) {
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

    private static Set<String> normalizeToolCodes(Set<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return new LinkedHashSet<>();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String code : codes) {
            if (code != null && !code.isBlank()) {
                normalized.add(code.trim().toUpperCase().replace('-', '_'));
            }
        }
        return normalized;
    }
}
