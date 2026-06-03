package com.lebhas.creativesaas.campaignpackage.application.dto;

import com.lebhas.creativesaas.campaignpackage.domain.BulkGenerationJobStatus;
import com.lebhas.creativesaas.campaignpackage.domain.BulkGenerationType;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record BulkGenerationJobView(
        UUID id,
        UUID workspaceId,
        UUID projectId,
        BulkGenerationType generationType,
        PromptPlatform platform,
        PromptLanguage language,
        int itemCount,
        BigDecimal estimatedCredits,
        BulkGenerationJobStatus status,
        Map<String, Object> requestPayload
) {
}
