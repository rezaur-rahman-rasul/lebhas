package com.lebhas.ai.application.dto;

import com.lebhas.ai.domain.CreativeLayerType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CreativePipelineLayerView(
        UUID id,
        UUID pipelineId,
        CreativeLayerType layerType,
        String layerCode,
        String layerName,
        int sortOrder,
        boolean enabled,
        boolean required,
        boolean retryable,
        Map<String, Object> configuration,
        List<LayerToolMappingView> toolMappings,
        List<LayerRoutingPolicyView> routingPolicies,
        List<LayerCostPolicyView> costPolicies,
        List<LayerQualityPolicyView> qualityPolicies
) {
}
