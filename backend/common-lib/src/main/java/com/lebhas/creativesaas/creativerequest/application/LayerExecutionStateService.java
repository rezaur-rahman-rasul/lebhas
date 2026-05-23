package com.lebhas.creativesaas.creativerequest.application;

import com.lebhas.ai.cache.AiLayerExecutionStateCacheService;
import com.lebhas.ai.cache.LayerExecutionStateCacheEntry;
import com.lebhas.ai.event.AiLayerLifecycleEvent;
import com.lebhas.ai.producer.AiCreativePipelineEventProducer;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class LayerExecutionStateService {

    private final AiLayerExecutionStateCacheService aiLayerExecutionStateCacheService;
    private final AiCreativePipelineEventProducer aiCreativePipelineEventProducer;

    public LayerExecutionStateService(
            AiLayerExecutionStateCacheService aiLayerExecutionStateCacheService,
            AiCreativePipelineEventProducer aiCreativePipelineEventProducer
    ) {
        this.aiLayerExecutionStateCacheService = aiLayerExecutionStateCacheService;
        this.aiCreativePipelineEventProducer = aiCreativePipelineEventProducer;
    }

    public void markStarted(CreativeRequestEntity request, UUID generationJobId, LayerRoutingDecision decision, int attempt) {
        store(request, generationJobId, decision, attempt, "STARTED", null, decision.metadata());
        aiCreativePipelineEventProducer.publishLayerStarted(event(request, generationJobId, decision, attempt, "STARTED", null, decision.metadata()));
    }

    public void markCompleted(CreativeRequestEntity request, UUID generationJobId, LayerRoutingDecision decision, int attempt, LayerExecutionResult result) {
        store(request, generationJobId, decision, attempt, "COMPLETED", result.message(), result.metadata());
        aiCreativePipelineEventProducer.publishLayerCompleted(event(request, generationJobId, decision, attempt, "COMPLETED", result.message(), result.metadata()));
    }

    public void markFailed(CreativeRequestEntity request, UUID generationJobId, LayerRoutingDecision decision, int attempt, String reason, Map<String, Object> metadata) {
        store(request, generationJobId, decision, attempt, "FAILED", reason, metadata);
        aiCreativePipelineEventProducer.publishLayerFailed(event(request, generationJobId, decision, attempt, "FAILED", reason, metadata));
    }

    private void store(
            CreativeRequestEntity request,
            UUID generationJobId,
            LayerRoutingDecision decision,
            int attempt,
            String state,
            String message,
            Map<String, Object> metadata
    ) {
        aiLayerExecutionStateCacheService.store(new LayerExecutionStateCacheEntry(
                request.getWorkspaceId(),
                request.getId(),
                decision.layer().getId(),
                decision.layer().getLayerType(),
                state,
                attempt,
                decision.candidate() == null ? null : decision.candidate().provider().getId(),
                decision.candidate() == null ? null : decision.candidate().mapping().getModelId(),
                message,
                metadata,
                Instant.now()));
    }

    private AiLayerLifecycleEvent event(
            CreativeRequestEntity request,
            UUID generationJobId,
            LayerRoutingDecision decision,
            int attempt,
            String status,
            String reason,
            Map<String, Object> metadata
    ) {
        return new AiLayerLifecycleEvent(
                null,
                null,
                request.getWorkspaceId(),
                request.getId(),
                null,
                generationJobId,
                decision.layer().getPipelineId(),
                decision.layer().getId(),
                decision.layer().getLayerType(),
                decision.candidate() == null ? null : decision.candidate().provider().getId(),
                null,
                attempt,
                status,
                reason,
                metadata);
    }
}
