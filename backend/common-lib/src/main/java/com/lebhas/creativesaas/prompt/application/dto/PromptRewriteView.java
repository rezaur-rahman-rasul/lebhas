package com.lebhas.creativesaas.prompt.application.dto;

import java.util.List;

public record PromptRewriteView(
        List<String> variations,
        String reasoningSummary,
        String aiProvider,
        String aiModel,
        Integer tokenUsage
) {
}
