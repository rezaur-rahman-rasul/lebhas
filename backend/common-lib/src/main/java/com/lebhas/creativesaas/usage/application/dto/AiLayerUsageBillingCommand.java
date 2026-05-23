package com.lebhas.creativesaas.usage.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AiLayerUsageBillingCommand(
        UUID workspaceId,
        UUID creativeRequestId,
        UUID generatedVersionId,
        UUID layerExecutionLogId,
        UUID layerId,
        UUID providerId,
        UUID modelId,
        String modelCode,
        BigDecimal requestedUnits,
        BigDecimal creditsCharged,
        LocalDate usageMonth
) {
}
