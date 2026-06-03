package com.lebhas.creativesaas.generation.application;

import com.lebhas.ai.cache.AiPipelineExecutionStateCacheService;
import com.lebhas.ai.cache.PipelineExecutionStateCacheEntry;
import com.lebhas.ai.domain.CreativePipelineLayer;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.creativerequest.application.LayerExecutionResult;
import com.lebhas.creativesaas.creativerequest.application.LayerExecutionStateService;
import com.lebhas.creativesaas.creativerequest.application.LayerProviderExecutionGateway;
import com.lebhas.creativesaas.creativerequest.application.LayerRoutingDecision;
import com.lebhas.creativesaas.creativerequest.application.LayerRoutingResolver;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CreativeLayerPipelineExecutor {

    private final LayerRoutingResolver layerRoutingResolver;
    private final LayerExecutionStateService layerExecutionStateService;
    private final LayerProviderExecutionGateway layerProviderExecutionGateway;
    private final AiPipelineExecutionStateCacheService pipelineExecutionStateCacheService;

    public CreativeLayerPipelineExecutor(
            LayerRoutingResolver layerRoutingResolver,
            LayerExecutionStateService layerExecutionStateService,
            LayerProviderExecutionGateway layerProviderExecutionGateway,
            AiPipelineExecutionStateCacheService pipelineExecutionStateCacheService
    ) {
        this.layerRoutingResolver = layerRoutingResolver;
        this.layerExecutionStateService = layerExecutionStateService;
        this.layerProviderExecutionGateway = layerProviderExecutionGateway;
        this.pipelineExecutionStateCacheService = pipelineExecutionStateCacheService;
    }

    public Map<String, Object> execute(GenerationExecutionContext executionContext) {
        storePipelineState(executionContext, "STARTED", null);
        Map<String, Object> layerOutputs = new LinkedHashMap<>();
        for (CreativePipelineLayer layer : executionContext.pipeline().layers()) {
            LayerRoutingDecision decision = layerRoutingResolver.resolve(
                    executionContext.pipeline(),
                    layer,
                    executionContext.request());
            layerExecutionStateService.markStarted(
                    executionContext.request(),
                    executionContext.job().getId(),
                    decision,
                    executionContext.job().getAttemptCount());
            LayerExecutionResult result = layerProviderExecutionGateway.executeFoundationLayer(
                    executionContext.request(),
                    executionContext.pipeline(),
                    decision);
            if (!result.success()) {
                layerExecutionStateService.markFailed(
                        executionContext.request(),
                        executionContext.job().getId(),
                        decision,
                        executionContext.job().getAttemptCount(),
                        result.message(),
                        result.metadata());
                storePipelineState(executionContext, "FAILED", layer.getLayerType().name());
                throw new BusinessException(ErrorCode.GENERATION_PROVIDER_REQUEST_FAILED, result.message());
            }
            layerExecutionStateService.markCompleted(
                    executionContext.request(),
                    executionContext.job().getId(),
                    decision,
                    executionContext.job().getAttemptCount(),
                    result);
            layerOutputs.put(layer.getLayerType().name(), result.metadata());
        }
        storePipelineState(executionContext, "FOUNDATION_COMPLETED", null);
        return layerOutputs;
    }

    private void storePipelineState(
            GenerationExecutionContext executionContext,
            String state,
            String currentLayerType
    ) {
        pipelineExecutionStateCacheService.store(new PipelineExecutionStateCacheEntry(
                executionContext.job().getWorkspaceId(),
                executionContext.request().getId(),
                executionContext.pipeline().pipeline().getId(),
                state,
                0,
                currentLayerType,
                Map.of("pipelineVersion", executionContext.pipeline().pipeline().getVersion()),
                Instant.now()));
    }
}
