package com.lebhas.ai.application;

import com.lebhas.ai.application.dto.CreativePipelineLayerView;
import com.lebhas.ai.application.dto.CreativePipelineView;
import com.lebhas.ai.application.dto.LayerCostPolicyView;
import com.lebhas.ai.application.dto.LayerQualityPolicyView;
import com.lebhas.ai.application.dto.LayerRoutingPolicyView;
import com.lebhas.ai.application.dto.LayerToolMappingView;
import com.lebhas.ai.domain.CreativePipeline;
import com.lebhas.ai.domain.CreativePipelineLayer;
import com.lebhas.ai.domain.LayerCostPolicy;
import com.lebhas.ai.domain.LayerQualityPolicy;
import com.lebhas.ai.domain.LayerRoutingPolicy;
import com.lebhas.ai.domain.LayerToolMapping;

import java.util.List;

public class CreativePipelineMapper {

    CreativePipelineView toView(CreativePipeline pipeline, List<CreativePipelineLayerView> layers) {
        return new CreativePipelineView(
                pipeline.getId(),
                pipeline.getPipelineCode(),
                pipeline.getPipelineName(),
                pipeline.getDescription(),
                pipeline.getStatus(),
                pipeline.isActive(),
                pipeline.getVersion(),
                pipeline.getMetadata(),
                layers);
    }

    CreativePipelineLayerView toView(
            CreativePipelineLayer layer,
            List<LayerToolMapping> toolMappings,
            List<LayerRoutingPolicy> routingPolicies,
            List<LayerCostPolicy> costPolicies,
            List<LayerQualityPolicy> qualityPolicies
    ) {
        return new CreativePipelineLayerView(
                layer.getId(),
                layer.getPipelineId(),
                layer.getLayerType(),
                layer.getLayerCode(),
                layer.getLayerName(),
                layer.getSortOrder(),
                layer.isEnabled(),
                layer.isRequired(),
                layer.isRetryable(),
                layer.getConfiguration(),
                toolMappings.stream().map(this::toView).toList(),
                routingPolicies.stream().map(this::toView).toList(),
                costPolicies.stream().map(this::toView).toList(),
                qualityPolicies.stream().map(this::toView).toList());
    }

    LayerToolMappingView toView(LayerToolMapping mapping) {
        return new LayerToolMappingView(
                mapping.getId(),
                mapping.getPipelineLayerId(),
                mapping.getProviderId(),
                mapping.getModelId(),
                mapping.getCapabilityId(),
                mapping.getMappingCode(),
                mapping.getPriorityOrder(),
                mapping.getRoutingWeight(),
                mapping.isEnabled(),
                mapping.isFallbackEligible(),
                mapping.getRoutingMetadata());
    }

    LayerRoutingPolicyView toView(LayerRoutingPolicy policy) {
        return new LayerRoutingPolicyView(
                policy.getId(),
                policy.getPipelineLayerId(),
                policy.getPolicyCode(),
                policy.getRoutingStrategy(),
                policy.getPriorityOrder(),
                policy.isEnabled(),
                policy.getConditions(),
                policy.getRules());
    }

    LayerCostPolicyView toView(LayerCostPolicy policy) {
        return new LayerCostPolicyView(
                policy.getId(),
                policy.getPipelineLayerId(),
                policy.getPolicyCode(),
                policy.isEnabled(),
                policy.getPriorityOrder(),
                policy.getCurrency(),
                policy.getMaxCostPerRun(),
                policy.getCostRules(),
                policy.getBudgetMetadata());
    }

    LayerQualityPolicyView toView(LayerQualityPolicy policy) {
        return new LayerQualityPolicyView(
                policy.getId(),
                policy.getPipelineLayerId(),
                policy.getPolicyCode(),
                policy.isEnabled(),
                policy.getPriorityOrder(),
                policy.getMinQualityScore(),
                policy.getQualityRules(),
                policy.getEvaluationMetadata());
    }
}
