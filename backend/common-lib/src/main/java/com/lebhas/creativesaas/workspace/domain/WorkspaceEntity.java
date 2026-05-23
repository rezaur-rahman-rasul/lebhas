package com.lebhas.creativesaas.workspace.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Locale;
import java.util.UUID;

@Entity
@Table(
        name = "workspaces",
        schema = "platform",
        uniqueConstraints = @UniqueConstraint(name = "uk_workspaces_slug", columnNames = "slug")
)
public class WorkspaceEntity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "slug", nullable = false, length = 120)
    private String slug;

    @Column(name = "logo_url", length = 300)
    private String logoUrl;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "industry", length = 80)
    private String industry;

    @Column(name = "timezone", nullable = false, length = 80)
    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false, length = 20)
    private WorkspaceLanguage language;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "country", nullable = false, length = 2)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WorkspaceStatus status;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(name = "owner_user_id", updatable = false)
    private UUID ownerUserId;

    protected WorkspaceEntity() {
    }

    public static WorkspaceEntity create(
            String name,
            String slug,
            String logoUrl,
            String description,
            String industry,
            String timezone,
            WorkspaceLanguage language,
            String currency,
            String country,
            UUID ownerId
    ) {
        WorkspaceEntity workspace = new WorkspaceEntity();
        workspace.name = normalizeRequired(name);
        workspace.slug = normalizeSlug(slug);
        workspace.logoUrl = normalizeNullable(logoUrl);
        workspace.description = normalizeNullable(description);
        workspace.industry = normalizeNullable(industry);
        workspace.timezone = normalizeRequired(timezone);
        workspace.language = language;
        workspace.currency = normalizeUpper(currency);
        workspace.country = normalizeUpper(country);
        workspace.status = WorkspaceStatus.ACTIVE;
        workspace.ownerId = ownerId;
        workspace.ownerUserId = ownerId;
        return workspace;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getDescription() {
        return description;
    }

    public String getIndustry() {
        return industry;
    }

    public String getTimezone() {
        return timezone;
    }

    public WorkspaceLanguage getLanguage() {
        return language;
    }

    public String getCurrency() {
        return currency;
    }

    public String getCountry() {
        return country;
    }

    public WorkspaceStatus getStatus() {
        return status;
    }

    public UUID getOwnerId() {
        return ownerUserId == null ? ownerId : ownerUserId;
    }

    public UUID getOwnerUserId() {
        return getOwnerId();
    }

    public void update(
            String name,
            String slug,
            String logoUrl,
            String description,
            String industry,
            String timezone,
            WorkspaceLanguage language,
            String currency,
            String country
    ) {
        this.name = normalizeRequired(name);
        this.slug = normalizeSlug(slug);
        this.logoUrl = normalizeNullable(logoUrl);
        this.description = normalizeNullable(description);
        this.industry = normalizeNullable(industry);
        this.timezone = normalizeRequired(timezone);
        this.language = language;
        this.currency = normalizeUpper(currency);
        this.country = normalizeUpper(country);
    }

    public void changeStatus(WorkspaceStatus status) {
        this.status = status;
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

    private static String normalizeUpper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeSlug(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
