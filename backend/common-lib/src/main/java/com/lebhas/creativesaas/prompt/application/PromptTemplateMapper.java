package com.lebhas.creativesaas.prompt.application;

import com.lebhas.creativesaas.prompt.application.dto.PromptTemplateView;
import com.lebhas.creativesaas.prompt.cache.dto.PromptTemplateCacheEntry;
import com.lebhas.creativesaas.prompt.domain.PromptTemplateEntity;
import org.springframework.stereotype.Component;

@Component
public class PromptTemplateMapper {

    public PromptTemplateView toView(PromptTemplateEntity entity) {
        return new PromptTemplateView(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getName(),
                entity.getCategory(),
                entity.getDescription(),
                entity.getPlatform(),
                entity.getCampaignObjective(),
                entity.getBusinessType(),
                entity.getLanguage(),
                entity.getTemplateText(),
                entity.isPublic(),
                entity.isSystemDefault(),
                entity.getStatus(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public PromptTemplateView toView(PromptTemplateCacheEntry entry) {
        return new PromptTemplateView(
                entry.id(),
                entry.workspaceId(),
                entry.name(),
                entry.category(),
                entry.description(),
                entry.platform(),
                entry.campaignObjective(),
                entry.businessType(),
                entry.language(),
                entry.templateBody(),
                entry.isPublic(),
                entry.systemDefault(),
                entry.status(),
                entry.createdBy(),
                entry.createdAt(),
                entry.updatedAt());
    }
}
