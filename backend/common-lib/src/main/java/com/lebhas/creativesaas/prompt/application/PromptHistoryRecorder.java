package com.lebhas.creativesaas.prompt.application;

import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptHistoryEntity;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import com.lebhas.creativesaas.prompt.domain.SuggestionType;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PromptHistoryRecorder {

    private final PromptHistoryService promptHistoryService;

    public PromptHistoryRecorder(PromptHistoryService promptHistoryService) {
        this.promptHistoryService = promptHistoryService;
    }

    public PromptHistoryEntity recordSuccess(
            UUID workspaceId,
            UUID userId,
            String sourcePrompt,
            String outputPayload,
            PromptLanguage language,
            PromptPlatform platform,
            CampaignObjective campaignObjective,
            String businessType,
            String brandContextSnapshot,
            SuggestionType suggestionType,
            String aiProvider,
            String aiModel,
            Integer tokenUsage
    ) {
        return promptHistoryService.recordSuccess(
                workspaceId,
                null,
                null,
                userId,
                sourcePrompt,
                outputPayload,
                language,
                platform,
                campaignObjective,
                businessType,
                brandContextSnapshot,
                suggestionType,
                aiProvider,
                aiModel,
                tokenUsage);
    }

    public PromptHistoryEntity recordSuccess(
            UUID workspaceId,
            UUID projectId,
            UUID creativeRequestId,
            UUID userId,
            String sourcePrompt,
            String outputPayload,
            PromptLanguage language,
            PromptPlatform platform,
            CampaignObjective campaignObjective,
            String businessType,
            String brandContextSnapshot,
            SuggestionType suggestionType,
            String aiProvider,
            String aiModel,
            Integer tokenUsage
    ) {
        return promptHistoryService.recordSuccess(
                workspaceId,
                projectId,
                creativeRequestId,
                userId,
                sourcePrompt,
                outputPayload,
                language,
                platform,
                campaignObjective,
                businessType,
                brandContextSnapshot,
                suggestionType,
                aiProvider,
                aiModel,
                tokenUsage);
    }

    public PromptHistoryEntity recordFailure(
            UUID workspaceId,
            UUID userId,
            String sourcePrompt,
            PromptLanguage language,
            PromptPlatform platform,
            CampaignObjective campaignObjective,
            String businessType,
            String brandContextSnapshot,
            SuggestionType suggestionType,
            String aiProvider,
            String aiModel
    ) {
        return promptHistoryService.recordFailure(
                workspaceId,
                null,
                null,
                userId,
                sourcePrompt,
                language,
                platform,
                campaignObjective,
                businessType,
                brandContextSnapshot,
                suggestionType,
                aiProvider,
                aiModel);
    }

    public PromptHistoryEntity recordFailure(
            UUID workspaceId,
            UUID projectId,
            UUID creativeRequestId,
            UUID userId,
            String sourcePrompt,
            PromptLanguage language,
            PromptPlatform platform,
            CampaignObjective campaignObjective,
            String businessType,
            String brandContextSnapshot,
            SuggestionType suggestionType,
            String aiProvider,
            String aiModel
    ) {
        return promptHistoryService.recordFailure(
                workspaceId,
                projectId,
                creativeRequestId,
                userId,
                sourcePrompt,
                language,
                platform,
                campaignObjective,
                businessType,
                brandContextSnapshot,
                suggestionType,
                aiProvider,
                aiModel);
    }
}
