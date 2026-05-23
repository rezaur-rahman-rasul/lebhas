package com.lebhas.creativesaas.generation.application.dto;

import com.lebhas.creativesaas.generation.domain.CreativeGenerationStatus;
import com.lebhas.creativesaas.generation.domain.CreativeOutputFormat;
import com.lebhas.creativesaas.generation.domain.CreativeType;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CreativeOutputView(
        UUID id,
        UUID workspaceId,
        UUID requestId,
        UUID generatedAssetId,
        CreativeType creativeType,
        PromptPlatform platform,
        CreativeOutputFormat outputFormat,
        Integer width,
        Integer height,
        Long duration,
        Long fileSize,
        String previewUrl,
        String downloadUrl,
        String caption,
        String headline,
        String ctaText,
        Map<String, Object> metadata,
        CreativeGenerationStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
