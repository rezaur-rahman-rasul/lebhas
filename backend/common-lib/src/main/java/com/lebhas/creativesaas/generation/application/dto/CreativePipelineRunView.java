package com.lebhas.creativesaas.generation.application.dto;

import com.lebhas.creativesaas.generation.domain.CreativePipelineRunStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CreativePipelineRunView(
        UUID creativeRequestId,
        UUID pipelineRunId,
        CreativePipelineRunStatus status,
        String strategy,
        String primaryProviderCode,
        Map<String, Object> planJson,
        BigDecimal estimatedCreditCost,
        BigDecimal actualCreditCost,
        String failureReason,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt,
        List<CreativePipelineLayerRunView> layers
) {
}
