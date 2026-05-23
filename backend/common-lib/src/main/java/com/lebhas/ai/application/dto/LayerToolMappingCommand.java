package com.lebhas.ai.application.dto;

import java.util.Map;
import java.util.UUID;

public record LayerToolMappingCommand(
        UUID providerId,
        UUID modelId,
        UUID capabilityId,
        String mappingCode,
        int priorityOrder,
        int routingWeight,
        boolean enabled,
        boolean fallbackEligible,
        Map<String, Object> routingMetadata
) {
}
