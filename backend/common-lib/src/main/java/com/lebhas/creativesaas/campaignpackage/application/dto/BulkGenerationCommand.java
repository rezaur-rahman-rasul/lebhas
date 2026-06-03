package com.lebhas.creativesaas.campaignpackage.application.dto;

import com.lebhas.creativesaas.campaignpackage.domain.BulkGenerationType;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record BulkGenerationCommand(
        UUID workspaceId,
        UUID projectId,
        BulkGenerationType generationType,
        PromptPlatform platform,
        PromptLanguage language,
        List<UUID> sourceIds,
        Map<String, Object> options
) {
}
