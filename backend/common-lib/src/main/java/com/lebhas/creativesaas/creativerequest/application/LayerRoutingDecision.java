package com.lebhas.creativesaas.creativerequest.application;

import com.lebhas.ai.domain.CreativePipelineLayer;
import com.lebhas.ai.domain.LayerCostPolicy;
import com.lebhas.ai.domain.LayerQualityPolicy;
import com.lebhas.ai.domain.LayerRoutingPolicy;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

public record LayerRoutingDecision(
        CreativePipelineLayer layer,
        Optional<LayerRoutingPolicy> routingPolicy,
        Optional<LayerCostPolicy> costPolicy,
        Optional<LayerQualityPolicy> qualityPolicy,
        LayerToolCandidate candidate,
        BigDecimal estimatedCost,
        BigDecimal qualityScore,
        Map<String, Object> metadata
) {
    public LayerRoutingDecision {
        routingPolicy = routingPolicy == null ? Optional.empty() : routingPolicy;
        costPolicy = costPolicy == null ? Optional.empty() : costPolicy;
        qualityPolicy = qualityPolicy == null ? Optional.empty() : qualityPolicy;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
