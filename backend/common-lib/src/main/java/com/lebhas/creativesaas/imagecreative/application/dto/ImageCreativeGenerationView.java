package com.lebhas.creativesaas.imagecreative.application.dto;

import com.lebhas.creativesaas.imagecreative.domain.ImageCreativeFormat;
import com.lebhas.creativesaas.imagecreative.domain.ImageCreativeGenerationStatus;
import com.lebhas.creativesaas.imagecreative.domain.ImageCreativeQualityMode;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ImageCreativeGenerationView(
        UUID id,
        UUID workspaceId,
        UUID projectId,
        UUID creativeRequestId,
        UUID brandId,
        UUID productServiceId,
        UUID productAssetId,
        String toolCode,
        ImageCreativeFormat creativeFormat,
        PromptPlatform platform,
        PromptLanguage language,
        ImageCreativeQualityMode qualityMode,
        int requestedVersionCount,
        BigDecimal creditCost,
        UUID creditReservationId,
        ImageCreativeGenerationStatus status,
        String failureReason,
        List<UUID> generatedVersionIds,
        Map<String, Object> request,
        Instant createdAt
) {
}
