package com.lebhas.ai.application.dto;

import java.util.Map;
import java.util.UUID;

public record AiToolCapabilityView(
        UUID id,
        UUID providerId,
        String capabilityCode,
        String layerCode,
        String modelCode,
        boolean enabled,
        Map<String, Object> metadata
) {
}
