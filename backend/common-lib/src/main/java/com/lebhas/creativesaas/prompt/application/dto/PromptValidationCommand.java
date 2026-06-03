package com.lebhas.creativesaas.prompt.application.dto;

import com.lebhas.creativesaas.prompt.domain.PromptLanguage;

import java.util.List;
import java.util.UUID;

public record PromptValidationCommand(
        UUID workspaceId,
        UUID projectId,
        String promptText,
        PromptLanguage language,
        List<UUID> assetIds,
        boolean requireEnhancement,
        boolean requireSuggestions,
        boolean requireTemplates
) {
}
