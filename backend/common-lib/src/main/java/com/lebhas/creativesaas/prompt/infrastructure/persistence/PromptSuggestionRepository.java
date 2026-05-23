package com.lebhas.creativesaas.prompt.infrastructure.persistence;

import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;
import com.lebhas.creativesaas.prompt.domain.PromptSuggestionEntity;
import com.lebhas.creativesaas.prompt.domain.SuggestionType;

import java.util.List;
import java.util.UUID;

public interface PromptSuggestionRepository extends TenantAwareRepository<PromptSuggestionEntity> {

    List<PromptSuggestionEntity> findAllByWorkspaceIdAndProjectCampaignIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID workspaceId,
            UUID projectCampaignId
    );

    List<PromptSuggestionEntity> findAllByWorkspaceIdAndSuggestionTypeAndDeletedFalseOrderByCreatedAtDesc(
            UUID workspaceId,
            SuggestionType suggestionType
    );
}
