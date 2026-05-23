package com.lebhas.creativesaas.creativerequest.application;

import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;

public interface LayerProviderExecutionGateway {

    LayerExecutionResult executeFoundationLayer(
            CreativeRequestEntity request,
            PipelineResolutionContext context,
            LayerRoutingDecision decision);
}
