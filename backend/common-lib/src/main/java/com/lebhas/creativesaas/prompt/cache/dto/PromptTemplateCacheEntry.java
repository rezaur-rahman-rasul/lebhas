package com.lebhas.creativesaas.prompt.cache.dto;

import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import com.lebhas.creativesaas.prompt.domain.PromptTemplateEntity;
import com.lebhas.creativesaas.prompt.domain.PromptTemplateStatus;

import java.time.Instant;
import java.util.UUID;

public record PromptTemplateCacheEntry(
        UUID id,
        UUID workspaceId,
        String name,
        String category,
        String description,
        PromptLanguage language,
        String templateBody,
        boolean isPublic,
        PromptPlatform platform,
        CampaignObjective campaignObjective,
        String businessType,
        boolean systemDefault,
        PromptTemplateStatus status,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {

    public static PromptTemplateCacheEntry from(PromptTemplateEntity entity) {
        if (entity == null) {
            return null;
        }
        return new PromptTemplateCacheEntry(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getName(),
                entity.getCategory(),
                entity.getDescription(),
                entity.getLanguage(),
                entity.getTemplateBody(),
                entity.isPublic(),
                entity.getPlatform(),
                entity.getCampaignObjective(),
                entity.getBusinessType(),
                entity.isSystemDefault(),
                entity.getStatus(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
