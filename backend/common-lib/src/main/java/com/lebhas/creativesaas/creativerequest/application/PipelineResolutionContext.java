package com.lebhas.creativesaas.creativerequest.application;

import com.lebhas.ai.domain.CreativePipeline;
import com.lebhas.ai.domain.CreativePipelineLayer;
import com.lebhas.creativesaas.pricing.application.dto.WorkspacePlanContextView;

import java.util.List;

public record PipelineResolutionContext(
        WorkspacePlanContextView planContext,
        CreativePipeline pipeline,
        List<CreativePipelineLayer> layers
) {
    public PipelineResolutionContext {
        layers = layers == null ? List.of() : List.copyOf(layers);
    }
}
