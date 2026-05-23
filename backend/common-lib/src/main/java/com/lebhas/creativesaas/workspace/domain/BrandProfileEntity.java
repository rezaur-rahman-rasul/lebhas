package com.lebhas.creativesaas.workspace.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(
        name = "brand_profiles",
        schema = "platform",
        uniqueConstraints = @UniqueConstraint(name = "uk_brand_profiles_workspace_id", columnNames = "workspace_id")
)
public class BrandProfileEntity extends TenantAwareEntity {

    @Column(name = "brand_name", nullable = false, length = 120)
    private String brandName;

    @Column(name = "business_type", length = 80)
    private String businessType;

    @Column(name = "industry", length = 80)
    private String industry;

    @Column(name = "target_audience", length = 160)
    private String targetAudience;

    @Column(name = "brand_voice", length = 120)
    private String brandVoice;

    @Column(name = "preferred_cta", length = 120)
    private String preferredCta;

    @Column(name = "primary_color", length = 7)
    private String primaryColor;

    @Column(name = "secondary_color", length = 7)
    private String secondaryColor;

    @Column(name = "website", length = 300)
    private String website;

    @Column(name = "facebook_url", length = 300)
    private String facebookUrl;

    @Column(name = "instagram_url", length = 300)
    private String instagramUrl;

    @Column(name = "linkedin_url", length = 300)
    private String linkedinUrl;

    @Column(name = "tiktok_url", length = 300)
    private String tiktokUrl;

    @Column(name = "description", length = 1000)
    private String description;

    protected BrandProfileEntity() {
    }

    public static BrandProfileEntity create(UUID workspaceId, String brandName, String industry) {
        BrandProfileEntity profile = new BrandProfileEntity();
        profile.assignWorkspace(workspaceId);
        profile.brandName = normalizeRequired(brandName);
        profile.industry = normalizeNullable(industry);
        return profile;
    }

    public String getBrandName() {
        return brandName;
    }

    public String getBusinessType() {
        return businessType;
    }

    public String getIndustry() {
        return industry;
    }

    public String getTargetAudience() {
        return targetAudience;
    }

    public String getBrandVoice() {
        return brandVoice;
    }

    public String getPreferredCta() {
        return preferredCta;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public String getSecondaryColor() {
        return secondaryColor;
    }

    public String getWebsite() {
        return website;
    }

    public String getFacebookUrl() {
        return facebookUrl;
    }

    public String getInstagramUrl() {
        return instagramUrl;
    }

    public String getLinkedinUrl() {
        return linkedinUrl;
    }

    public String getTiktokUrl() {
        return tiktokUrl;
    }

    public String getDescription() {
        return description;
    }

    public void update(
            String brandName,
            String businessType,
            String industry,
            String targetAudience,
            String brandVoice,
            String preferredCta,
            String primaryColor,
            String secondaryColor,
            String website,
            String facebookUrl,
            String instagramUrl,
            String linkedinUrl,
            String tiktokUrl,
            String description
    ) {
        this.brandName = normalizeRequired(brandName);
        this.businessType = normalizeNullable(businessType);
        this.industry = normalizeNullable(industry);
        this.targetAudience = normalizeNullable(targetAudience);
        this.brandVoice = normalizeNullable(brandVoice);
        this.preferredCta = normalizeNullable(preferredCta);
        this.primaryColor = normalizeNullable(primaryColor);
        this.secondaryColor = normalizeNullable(secondaryColor);
        this.website = normalizeNullable(website);
        this.facebookUrl = normalizeNullable(facebookUrl);
        this.instagramUrl = normalizeNullable(instagramUrl);
        this.linkedinUrl = normalizeNullable(linkedinUrl);
        this.tiktokUrl = normalizeNullable(tiktokUrl);
        this.description = normalizeNullable(description);
    }

    private static String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
