package com.lebhas.creativesaas.prompt.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "prompt_suggestions", schema = "platform")
public class PromptSuggestionEntity extends TenantAwareEntity {

    @Column(name = "project_campaign_id")
    private UUID projectCampaignId;

    @Enumerated(EnumType.STRING)
    @Column(name = "suggestion_type", nullable = false, length = 40)
    private SuggestionType suggestionType;

    @Column(name = "suggestion_text", nullable = false, columnDefinition = "TEXT")
    private String suggestionText;

    protected PromptSuggestionEntity() {
    }

    public static PromptSuggestionEntity create(
            UUID workspaceId,
            UUID projectCampaignId,
            SuggestionType suggestionType,
            String suggestionText
    ) {
        PromptSuggestionEntity entity = new PromptSuggestionEntity();
        entity.assignWorkspace(workspaceId);
        entity.projectCampaignId = projectCampaignId;
        entity.suggestionType = requireSuggestionType(suggestionType);
        entity.suggestionText = normalizeRequired(suggestionText);
        return entity;
    }

    public UUID getProjectCampaignId() {
        return projectCampaignId;
    }

    public SuggestionType getSuggestionType() {
        return suggestionType;
    }

    public String getSuggestionText() {
        return suggestionText;
    }

    private static SuggestionType requireSuggestionType(SuggestionType suggestionType) {
        if (suggestionType == null) {
            throw new IllegalArgumentException("suggestionType must not be null");
        }
        return suggestionType;
    }

    private static String normalizeRequired(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("suggestionText must not be blank");
        }
        return value.trim();
    }
}
