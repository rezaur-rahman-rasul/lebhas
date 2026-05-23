package com.lebhas.creativesaas.prompt.application;

import com.lebhas.creativesaas.prompt.application.dto.PromptSuggestionCommand;
import com.lebhas.creativesaas.prompt.application.dto.PromptSuggestionListView;
import com.lebhas.creativesaas.prompt.application.dto.PromptSuggestionsView;
import com.lebhas.creativesaas.prompt.domain.SuggestionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PromptSuggestionService {

    private final PromptIntelligenceService promptIntelligenceService;

    public PromptSuggestionService(PromptIntelligenceService promptIntelligenceService) {
        this.promptIntelligenceService = promptIntelligenceService;
    }

    @Transactional
    public PromptSuggestionsView generateSuggestions(PromptSuggestionCommand command) {
        return promptIntelligenceService.generateSuggestions(command);
    }

    @Transactional
    public PromptSuggestionListView generateSuggestionList(PromptSuggestionCommand command, SuggestionType suggestionType) {
        return promptIntelligenceService.generateSuggestionList(command, suggestionType);
    }
}
