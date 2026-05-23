package com.lebhas.creativesaas.prompt.application.dto;

import java.util.List;

public record PromptEnhancementView(
        String enhancedPrompt,
        String reasoningSummary,
        List<String> suggestedMissingFields,
        String aiProvider,
        String aiModel,
        Integer tokenUsage
) {
}
