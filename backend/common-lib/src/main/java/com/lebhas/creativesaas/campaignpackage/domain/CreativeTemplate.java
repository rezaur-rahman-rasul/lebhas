package com.lebhas.creativesaas.campaignpackage.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "creative_templates", schema = "platform")
public class CreativeTemplate extends TenantAwareEntity {

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private CreativeTemplateCategory category;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", length = 40)
    private PromptPlatform platform;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", length = 40)
    private PromptLanguage language;

    @Enumerated(EnumType.STRING)
    @Column(name = "campaign_objective", length = 40)
    private CampaignObjective campaignObjective;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "master_template", nullable = false)
    private boolean masterTemplate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "template_payload", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> templatePayload = new LinkedHashMap<>();

    protected CreativeTemplate() {
    }

    public static CreativeTemplate create(UUID workspaceId, String name, CreativeTemplateCategory category, String description,
                                          PromptPlatform platform, PromptLanguage language, CampaignObjective objective,
                                          boolean masterTemplate, Map<String, Object> payload, CreativeTemplateStatus status) {
        CreativeTemplate template = new CreativeTemplate();
        template.assignWorkspace(workspaceId);
        template.name = required(name, "name");
        template.category = category == null ? CreativeTemplateCategory.CUSTOM : category;
        template.description = normalize(description);
        template.platform = platform;
        template.language = language;
        template.campaignObjective = objective;
        template.masterTemplate = masterTemplate;
        template.active = status == null || status == CreativeTemplateStatus.ACTIVE;
        template.templatePayload = new LinkedHashMap<>(payload == null ? Map.of() : payload);
        return template;
    }

    public void update(String name, CreativeTemplateCategory category, String description, PromptPlatform platform,
                       PromptLanguage language, CampaignObjective objective, Map<String, Object> payload,
                       CreativeTemplateStatus status) {
        this.name = required(name, "name");
        this.category = category == null ? CreativeTemplateCategory.CUSTOM : category;
        this.description = normalize(description);
        this.platform = platform;
        this.language = language;
        this.campaignObjective = objective;
        this.templatePayload = new LinkedHashMap<>(payload == null ? Map.of() : payload);
        this.active = status == null || status == CreativeTemplateStatus.ACTIVE;
    }

    public boolean accessibleTo(UUID workspaceId) {
        return masterTemplate || getWorkspaceId().equals(workspaceId);
    }

    public String getName() { return name; }
    public CreativeTemplateCategory getCategory() { return category; }
    public String getDescription() { return description; }
    public PromptPlatform getPlatform() { return platform; }
    public PromptLanguage getLanguage() { return language; }
    public CampaignObjective getCampaignObjective() { return campaignObjective; }
    public boolean isActive() { return active; }
    public boolean isMasterTemplate() { return masterTemplate; }
    public Map<String, Object> getTemplatePayload() { return Map.copyOf(templatePayload); }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
