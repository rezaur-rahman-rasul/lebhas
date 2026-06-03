package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record PromptValidateRequest(
        @Size(max = 5000)
        String promptText,
        PromptLanguage language,
        List<UUID> assetIds,
        boolean requireEnhancement,
        boolean requireSuggestions,
        boolean requireTemplates
) {
}
