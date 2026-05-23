package com.lebhas.creativesaas.brand.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "brands", schema = "platform")
public class BrandEntity extends TenantAwareEntity {

    @Column(name = "owner_user_id", nullable = false, updatable = false)
    private UUID ownerUserId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "language_preference", nullable = false, length = 20)
    private BrandLanguagePreference languagePreference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BrandStatus status;

    protected BrandEntity() {
    }

    public static BrandEntity create(
            UUID workspaceId,
            UUID ownerUserId,
            String name,
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
            BrandLanguagePreference languagePreference
    ) {
        BrandEntity brand = new BrandEntity();
        brand.assignWorkspace(workspaceId);
        brand.ownerUserId = requireOwner(ownerUserId);
        brand.name = normalizeRequired(name);
        brand.businessType = normalizeNullable(businessType);
        brand.industry = normalizeNullable(industry);
        brand.targetAudience = normalizeNullable(targetAudience);
        brand.brandVoice = normalizeNullable(brandVoice);
        brand.preferredCta = normalizeNullable(preferredCta);
        brand.primaryColor = normalizeNullable(primaryColor);
        brand.secondaryColor = normalizeNullable(secondaryColor);
        brand.website = normalizeNullable(website);
        brand.facebookUrl = normalizeNullable(facebookUrl);
        brand.instagramUrl = normalizeNullable(instagramUrl);
        brand.linkedinUrl = normalizeNullable(linkedinUrl);
        brand.tiktokUrl = normalizeNullable(tiktokUrl);
        brand.languagePreference = normalizeLanguagePreference(languagePreference);
        brand.status = BrandStatus.ACTIVE;
        return brand;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public String getName() {
        return name;
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

    public BrandLanguagePreference getLanguagePreference() {
        return languagePreference;
    }

    public BrandStatus getStatus() {
        return status;
    }

    public void update(
            String name,
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
            BrandLanguagePreference languagePreference
    ) {
        this.name = normalizeRequired(name);
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
        this.languagePreference = languagePreference == null
                ? this.languagePreference == null ? BrandLanguagePreference.BOTH : this.languagePreference
                : languagePreference;
    }

    public void changeStatus(BrandStatus status) {
        this.status = status == null ? BrandStatus.ACTIVE : status;
    }

    private static UUID requireOwner(UUID ownerUserId) {
        if (ownerUserId == null) {
            throw new IllegalArgumentException("ownerUserId must not be null");
        }
        return ownerUserId;
    }

    private static String normalizeRequired(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Brand name must not be blank");
        }
        return value.trim();
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static BrandLanguagePreference normalizeLanguagePreference(BrandLanguagePreference languagePreference) {
        return languagePreference == null ? BrandLanguagePreference.BOTH : languagePreference;
    }
}
