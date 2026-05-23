package com.lebhas.creativesaas.prompt.application.dto;

import java.util.List;

public record PromptSuggestionsView(
        List<String> ctaSuggestions,
        List<String> headlineSuggestions,
        List<String> offerSuggestions,
        List<String> creativeAngleSuggestions,
        List<String> campaignToneSuggestions,
        List<String> businessCategorySuggestions,
        String reasoningSummary,
        String aiProvider,
        String aiModel,
        Integer tokenUsage
) {
}
