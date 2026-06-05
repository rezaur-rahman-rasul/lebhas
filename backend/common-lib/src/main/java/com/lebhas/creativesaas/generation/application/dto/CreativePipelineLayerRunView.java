package com.lebhas.creativesaas.generation.application.dto;

import com.lebhas.ai.domain.CreativeLayerType;
import com.lebhas.creativesaas.generation.domain.CreativePipelineRunStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CreativePipelineLayerRunView(
        UUID id,
        int sequence,
        CreativeLayerType layerType,
        String providerCode,
        String modelCode,
        CreativePipelineRunStatus status,
        Map<String, Object> inputJson,
        Map<String, Object> outputJson,
        List<UUID> inputAssetIds,
        List<UUID> outputAssetIds,
        BigDecimal estimatedCost,
        BigDecimal actualCost,
        Instant startedAt,
        Instant completedAt,
        String failureReason
) {
}
