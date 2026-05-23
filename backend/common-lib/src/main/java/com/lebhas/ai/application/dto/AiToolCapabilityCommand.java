package com.lebhas.ai.application.dto;

import java.util.Map;

public record AiToolCapabilityCommand(
        String capabilityCode,
        String layerCode,
        String modelCode,
        boolean enabled,
        Map<String, Object> metadata
) {
}
