package com.lebhas.creativesaas.generation.application.dto;

import com.lebhas.creativesaas.generation.domain.CreativeGenerationStatus;
import com.lebhas.creativesaas.generation.domain.CreativeType;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;

import java.util.UUID;

public record CreativeGenerationListCriteria(
        UUID workspaceId,
        UUID userId,
        CreativeGenerationStatus status,
        CreativeType creativeType,
        PromptPlatform platform,
        int page,
        int size
) {
}
