package com.lebhas.creativesaas.creativerequest.application;

import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FoundationLayerProviderExecutionGateway implements LayerProviderExecutionGateway {

    @Override
    public LayerExecutionResult executeFoundationLayer(
            CreativeRequestEntity request,
            PipelineResolutionContext context,
            LayerRoutingDecision decision
    ) {
        return LayerExecutionResult.foundationSuccess(
                "Foundation layer resolved without provider execution",
                Map.of(
                        "foundationOnly", true,
                        "pipelineId", context.pipeline().getId(),
                        "layerType", decision.layer().getLayerType().name()));
    }
}
