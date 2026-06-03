package com.lebhas.creativesaas.prompt.application.dto;

import java.util.UUID;

public record PromptTemplateReuseView(
        UUID templateId,
        String templateText,
        PromptDraftView draft
) {
}
