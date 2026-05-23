package com.lebhas.creativesaas.usage.application;

import com.lebhas.ai.domain.AiModel;
import com.lebhas.ai.domain.AiToolProvider;
import com.lebhas.creativesaas.usage.application.dto.AiCostUsageView;
import com.lebhas.creativesaas.usage.domain.UsageBillingLog;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class AiCostUsageMapper {

    AiCostUsageView toLayerView(
            UsageBillingLog log,
            UUID creativeRequestId,
            UUID generatedVersionId,
            UUID layerExecutionLogId,
            UUID layerId,
            AiToolProvider provider,
            AiModel model,
            LocalDate usageMonth,
            BigDecimal requestedUnits
    ) {
        return new AiCostUsageView(
                log.getId(),
                log.getWorkspaceId(),
                creativeRequestId,
                generatedVersionId,
                layerExecutionLogId,
                layerId,
                provider == null ? null : provider.getId(),
                provider == null ? null : provider.getProviderCode(),
                model == null ? null : model.getId(),
                model == null ? null : model.getModelCode(),
                usageMonth,
                requestedUnits,
                log.getEstimatedCostUsd(),
                log.getCreditsCharged(),
                log.getCreatedAt());
    }

    AiCostUsageView toGenerationView(
            UsageBillingLog log,
            UUID creativeRequestId,
            UUID generatedVersionId,
            LocalDate usageMonth
    ) {
        return new AiCostUsageView(
                log.getId(),
                log.getWorkspaceId(),
                creativeRequestId,
                generatedVersionId,
                null,
                null,
                null,
                null,
                null,
                null,
                usageMonth,
                null,
                log.getEstimatedCostUsd(),
                log.getCreditsCharged(),
                log.getCreatedAt());
    }
}
