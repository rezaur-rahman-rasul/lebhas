package com.lebhas.creativesaas.generation.application;

import com.lebhas.ai.cache.AiPipelineExecutionStateCacheService;
import com.lebhas.ai.cache.PipelineExecutionStateCacheEntry;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.creativerequest.application.LayerExecutionResult;
import com.lebhas.creativesaas.creativerequest.application.LayerExecutionStateService;
import com.lebhas.creativesaas.creativerequest.application.LayerProviderExecutionGateway;
import com.lebhas.creativesaas.creativerequest.application.LayerRoutingDecision;
import com.lebhas.creativesaas.creativerequest.application.LayerRoutingResolver;
import com.lebhas.creativesaas.creativerequest.application.PipelineResolutionContext;
import com.lebhas.creativesaas.creativerequest.application.PipelineResolver;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.creativerequest.infrastructure.persistence.CreativeRequestRepository;
import com.lebhas.creativesaas.generation.domain.GenerationJobEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class GenerationFoundationService {

    private final CreativeRequestRepository creativeRequestRepository;
    private final PipelineResolver pipelineResolver;
    private final LayerRoutingResolver layerRoutingResolver;
    private final LayerExecutionStateService layerExecutionStateService;
    private final LayerProviderExecutionGateway layerProviderExecutionGateway;
    private final AiPipelineExecutionStateCacheService pipelineExecutionStateCacheService;

    public GenerationFoundationService(
            CreativeRequestRepository creativeRequestRepository,
            PipelineResolver pipelineResolver,
            LayerRoutingResolver layerRoutingResolver,
            LayerExecutionStateService layerExecutionStateService,
            LayerProviderExecutionGateway layerProviderExecutionGateway,
            AiPipelineExecutionStateCacheService pipelineExecutionStateCacheService
    ) {
        this.creativeRequestRepository = creativeRequestRepository;
        this.pipelineResolver = pipelineResolver;
        this.layerRoutingResolver = layerRoutingResolver;
        this.layerExecutionStateService = layerExecutionStateService;
        this.layerProviderExecutionGateway = layerProviderExecutionGateway;
        this.pipelineExecutionStateCacheService = pipelineExecutionStateCacheService;
    }

    @Transactional
    public void runFoundation(GenerationJobEntity job) {
        CreativeRequestEntity request = creativeRequestRepository
                .findByIdAndWorkspaceIdAndDeletedFalse(job.getCreativeRequestId(), job.getWorkspaceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CREATIVE_REQUEST_FOUNDATION_NOT_FOUND));
        PipelineResolutionContext context = pipelineResolver.resolve(job.getWorkspaceId());
        storePipelineState(job.getWorkspaceId(), request.getId(), context, "STARTED", null);
        for (com.lebhas.ai.domain.CreativePipelineLayer layer : context.layers()) {
            LayerRoutingDecision decision = layerRoutingResolver.resolve(context, layer, request);
            layerExecutionStateService.markStarted(request, job.getId(), decision, job.getAttemptCount());
            LayerExecutionResult result = layerProviderExecutionGateway.executeFoundationLayer(request, context, decision);
            if (!result.success()) {
                layerExecutionStateService.markFailed(request, job.getId(), decision, job.getAttemptCount(), result.message(), result.metadata());
                storePipelineState(job.getWorkspaceId(), request.getId(), context, "FAILED", layer.getLayerType().name());
                throw new BusinessException(ErrorCode.GENERATION_PROVIDER_REQUEST_FAILED, result.message());
            }
            layerExecutionStateService.markCompleted(request, job.getId(), decision, job.getAttemptCount(), result);
        }
        storePipelineState(job.getWorkspaceId(), request.getId(), context, "FOUNDATION_COMPLETED", null);
    }

    private void storePipelineState(
            UUID workspaceId,
            UUID creativeRequestId,
            PipelineResolutionContext context,
            String state,
            String currentLayerType
    ) {
        pipelineExecutionStateCacheService.store(new PipelineExecutionStateCacheEntry(
                workspaceId,
                creativeRequestId,
                context.pipeline().getId(),
                state,
                0,
                currentLayerType,
                Map.of("pipelineVersion", context.pipeline().getVersion()),
                Instant.now()));
    }
}
