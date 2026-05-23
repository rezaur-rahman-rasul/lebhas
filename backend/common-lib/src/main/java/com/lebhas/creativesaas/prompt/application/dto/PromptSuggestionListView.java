package com.lebhas.creativesaas.prompt.application.dto;

import com.lebhas.creativesaas.prompt.domain.SuggestionType;

import java.util.List;

public record PromptSuggestionListView(
        SuggestionType suggestionType,
        List<String> suggestions,
        String reasoningSummary,
        String aiProvider,
        String aiModel,
        Integer tokenUsage
) {
}
