package com.lebhas.ai.creative.dto;

import com.lebhas.ai.creative.enums.CreativeStatus;
import com.lebhas.ai.creative.enums.GenerationMode;
import com.lebhas.ai.creative.enums.OutputFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AiCreativeResponse(
        UUID creativeId,
        UUID workspaceId,
        UUID brandId,
        UUID campaignId,
        CreativeStatus status,
        GenerationMode generationMode,
        String provider,
        String model,
        String size,
        String quality,
        OutputFormat outputFormat,
        String background,
        String fileUrl,
        String r2ObjectKey,
        BigDecimal costEstimate,
        BigDecimal creditUsed,
        Instant createdAt,
        Instant completedAt
) {
}
