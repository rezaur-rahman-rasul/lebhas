package com.lebhas.creativesaas.prompt.application;

import com.lebhas.creativesaas.prompt.application.dto.PromptHistoryView;
import com.lebhas.creativesaas.prompt.application.dto.PromptTemplateView;
import com.lebhas.creativesaas.prompt.domain.PromptHistoryEntity;
import com.lebhas.creativesaas.prompt.domain.PromptTemplateEntity;
import org.springframework.stereotype.Component;

@Component
public class PromptViewMapper {

    private final PromptJsonCodec promptJsonCodec;
    private final PromptTemplateMapper promptTemplateMapper;

    public PromptViewMapper(
            PromptJsonCodec promptJsonCodec,
            PromptTemplateMapper promptTemplateMapper
    ) {
        this.promptJsonCodec = promptJsonCodec;
        this.promptTemplateMapper = promptTemplateMapper;
    }

    public PromptTemplateView toTemplateView(PromptTemplateEntity entity) {
        return promptTemplateMapper.toView(entity);
    }

    public PromptHistoryView toHistoryView(PromptHistoryEntity entity) {
        return new PromptHistoryView(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getProjectCampaignId() == null ? entity.getProjectId() : entity.getProjectCampaignId(),
                entity.getUserId(),
                entity.getSourcePrompt(),
                entity.getEnhancedPrompt(),
                entity.getLanguage(),
                entity.getPlatform(),
                entity.getCampaignObjective(),
                entity.getBusinessType(),
                promptJsonCodec.readMapQuietly(entity.getBrandContextSnapshot()),
                entity.getSuggestionType(),
                entity.getAiProvider(),
                entity.getAiModel(),
                entity.getTokenUsage(),
                entity.getStatus(),
                entity.getCreatedAt());
    }
}
