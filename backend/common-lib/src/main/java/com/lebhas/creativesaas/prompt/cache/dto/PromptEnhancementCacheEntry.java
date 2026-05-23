package com.lebhas.creativesaas.prompt.cache.dto;

import java.time.Instant;
import java.util.List;

public record PromptEnhancementCacheEntry(
        String promptHash,
        String enhancedPrompt,
        String reasoningSummary,
        List<String> suggestedMissingFields,
        String aiProvider,
        String aiModel,
        Integer tokenUsage,
        Instant cachedAt
) {
}
