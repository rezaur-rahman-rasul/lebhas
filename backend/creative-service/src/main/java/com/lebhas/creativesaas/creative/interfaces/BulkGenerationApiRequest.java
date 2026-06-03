package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.campaignpackage.domain.BulkGenerationType;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record BulkGenerationApiRequest(
        @NotNull BulkGenerationType generationType,
        PromptPlatform platform,
        PromptLanguage language,
        List<UUID> sourceIds,
        Map<String, Object> options
) {
}
