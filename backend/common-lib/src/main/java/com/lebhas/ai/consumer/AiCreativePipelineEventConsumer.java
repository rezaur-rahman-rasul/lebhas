package com.lebhas.ai.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.ai.event.AiCostEvent;
import com.lebhas.ai.event.AiCreditEvent;
import com.lebhas.ai.event.AiGenerationLifecycleEvent;
import com.lebhas.ai.event.AiLayerLifecycleEvent;
import com.lebhas.ai.event.AiLayerUpdatedEvent;
import com.lebhas.ai.event.AiPipelineUpdatedEvent;
import com.lebhas.ai.event.AiProviderUpdatedEvent;
import com.lebhas.ai.event.AiRoutingResolvedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

public class AiCreativePipelineEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AiCreativePipelineEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final AiCreativePipelineEventHooks hooks;

    public AiCreativePipelineEventConsumer(ObjectMapper objectMapper, AiCreativePipelineEventHooks hooks) {
        this.objectMapper = objectMapper;
        this.hooks = hooks;
    }

    @KafkaListener(topics = "#{@aiCreativePipelineKafkaTopicNames.aiPipelineUpdated()}", containerFactory = "kafkaListenerContainerFactory")
    public void consumePipelineUpdated(Object payload) {
        AiPipelineUpdatedEvent event = convert(payload, AiPipelineUpdatedEvent.class);
        log.debug("Received AI pipeline updated event pipelineId={}", event.pipelineId());
        hooks.onPipelineUpdated(event);
    }

    @KafkaListener(topics = "#{@aiCreativePipelineKafkaTopicNames.aiLayerUpdated()}", containerFactory = "kafkaListenerContainerFactory")
    public void consumeLayerUpdated(Object payload) {
        AiLayerUpdatedEvent event = convert(payload, AiLayerUpdatedEvent.class);
        log.debug("Received AI layer updated event pipelineId={} layerId={} layerType={}", event.pipelineId(), event.layerId(), event.layerType());
        hooks.onLayerUpdated(event);
    }

    @KafkaListener(topics = "#{@aiCreativePipelineKafkaTopicNames.aiProviderUpdated()}", containerFactory = "kafkaListenerContainerFactory")
    public void consumeProviderUpdated(Object payload) {
        AiProviderUpdatedEvent event = convert(payload, AiProviderUpdatedEvent.class);
        log.debug("Received AI provider updated event providerId={} providerCode={}", event.providerId(), event.providerCode());
        hooks.onProviderUpdated(event);
    }

    @KafkaListener(topics = "#{@aiCreativePipelineKafkaTopicNames.aiRoutingResolved()}", containerFactory = "kafkaListenerContainerFactory")
    public void consumeRoutingResolved(Object payload) {
        AiRoutingResolvedEvent event = convert(payload, AiRoutingResolvedEvent.class);
        log.debug("Received AI routing resolved event workspaceId={} requestId={} layerType={}", event.workspaceId(), event.creativeRequestId(), event.layerType());
        hooks.onRoutingResolved(event);
    }

    @KafkaListener(topics = "#{@aiCreativePipelineKafkaTopicNames.aiGenerationRequested()}", containerFactory = "kafkaListenerContainerFactory")
    public void consumeGenerationRequested(Object payload) {
        AiGenerationLifecycleEvent event = convert(payload, AiGenerationLifecycleEvent.class);
        log.debug("Received AI generation requested event workspaceId={} requestId={}", event.workspaceId(), event.creativeRequestId());
        hooks.onGenerationRequested(event);
    }

    @KafkaListener(topics = "#{@aiCreativePipelineKafkaTopicNames.aiGenerationStarted()}", containerFactory = "kafkaListenerContainerFactory")
    public void consumeGenerationStarted(Object payload) {
        AiGenerationLifecycleEvent event = convert(payload, AiGenerationLifecycleEvent.class);
        log.debug("Received AI generation started event workspaceId={} requestId={}", event.workspaceId(), event.creativeRequestId());
        hooks.onGenerationStarted(event);
    }

    @KafkaListener(topics = "#{@aiCreativePipelineKafkaTopicNames.aiGenerationCompleted()}", containerFactory = "kafkaListenerContainerFactory")
    public void consumeGenerationCompleted(Object payload) {
        AiGenerationLifecycleEvent event = convert(payload, AiGenerationLifecycleEvent.class);
        log.debug("Received AI generation completed event workspaceId={} requestId={}", event.workspaceId(), event.creativeRequestId());
        hooks.onGenerationCompleted(event);
    }

    @KafkaListener(topics = "#{@aiCreativePipelineKafkaTopicNames.aiGenerationFailed()}", containerFactory = "kafkaListenerContainerFactory")
    public void consumeGenerationFailed(Object payload) {
        AiGenerationLifecycleEvent event = convert(payload, AiGenerationLifecycleEvent.class);
        log.debug("Received AI generation failed event workspaceId={} requestId={}", event.workspaceId(), event.creativeRequestId());
        hooks.onGenerationFailed(event);
    }

    @KafkaListener(topics = "#{@aiCreativePipelineKafkaTopicNames.aiLayerStarted()}", containerFactory = "kafkaListenerContainerFactory")
    public void consumeLayerStarted(Object payload) {
        AiLayerLifecycleEvent event = convert(payload, AiLayerLifecycleEvent.class);
        log.debug("Received AI layer started event workspaceId={} requestId={} layerType={}", event.workspaceId(), event.creativeRequestId(), event.layerType());
        hooks.onLayerStarted(event);
    }

    @KafkaListener(topics = "#{@aiCreativePipelineKafkaTopicNames.aiLayerCompleted()}", containerFactory = "kafkaListenerContainerFactory")
    public void consumeLayerCompleted(Object payload) {
        AiLayerLifecycleEvent event = convert(payload, AiLayerLifecycleEvent.class);
        log.debug("Received AI layer completed event workspaceId={} requestId={} layerType={}", event.workspaceId(), event.creativeRequestId(), event.layerType());
        hooks.onLayerCompleted(event);
    }

    @KafkaListener(topics = "#{@aiCreativePipelineKafkaTopicNames.aiLayerFailed()}", containerFactory = "kafkaListenerContainerFactory")
    public void consumeLayerFailed(Object payload) {
        AiLayerLifecycleEvent event = convert(payload, AiLayerLifecycleEvent.class);
        log.debug("Received AI layer failed event workspaceId={} requestId={} layerType={}", event.workspaceId(), event.creativeRequestId(), event.layerType());
        hooks.onLayerFailed(event);
    }

    @KafkaListener(topics = "#{@aiCreativePipelineKafkaTopicNames.aiLayerFallbackUsed()}", containerFactory = "kafkaListenerContainerFactory")
    public void consumeLayerFallbackUsed(Object payload) {
        AiLayerLifecycleEvent event = convert(payload, AiLayerLifecycleEvent.class);
        log.debug("Received AI layer fallback used event workspaceId={} requestId={} layerType={}", event.workspaceId(), event.creativeRequestId(), event.layerType());
        hooks.onLayerFallbackUsed(event);
    }

    @KafkaListener(topics = "#{@aiCreativePipelineKafkaTopicNames.aiCostEstimated()}", containerFactory = "kafkaListenerContainerFactory")
    public void consumeCostEstimated(Object payload) {
        AiCostEvent event = convert(payload, AiCostEvent.class);
        log.debug("Received AI cost estimated event workspaceId={} requestId={} amount={}", event.workspaceId(), event.creativeRequestId(), event.amount());
        hooks.onCostEstimated(event);
    }

    @KafkaListener(topics = "#{@aiCreativePipelineKafkaTopicNames.aiCostFinalized()}", containerFactory = "kafkaListenerContainerFactory")
    public void consumeCostFinalized(Object payload) {
        AiCostEvent event = convert(payload, AiCostEvent.class);
        log.debug("Received AI cost finalized event workspaceId={} requestId={} amount={}", event.workspaceId(), event.creativeRequestId(), event.amount());
        hooks.onCostFinalized(event);
    }

    @KafkaListener(topics = "#{@aiCreativePipelineKafkaTopicNames.creditsReserved()}", containerFactory = "kafkaListenerContainerFactory")
    public void consumeCreditsReserved(Object payload) {
        AiCreditEvent event = convert(payload, AiCreditEvent.class);
        log.debug("Received credits reserved event workspaceId={} requestId={} reservationId={}", event.workspaceId(), event.creativeRequestId(), event.creditReservationId());
        hooks.onCreditsReserved(event);
    }

    @KafkaListener(topics = "#{@aiCreativePipelineKafkaTopicNames.creditsFinalized()}", containerFactory = "kafkaListenerContainerFactory")
    public void consumeCreditsFinalized(Object payload) {
        AiCreditEvent event = convert(payload, AiCreditEvent.class);
        log.debug("Received credits finalized event workspaceId={} requestId={} reservationId={}", event.workspaceId(), event.creativeRequestId(), event.creditReservationId());
        hooks.onCreditsFinalized(event);
    }

    @KafkaListener(topics = "#{@aiCreativePipelineKafkaTopicNames.creditsRefunded()}", containerFactory = "kafkaListenerContainerFactory")
    public void consumeCreditsRefunded(Object payload) {
        AiCreditEvent event = convert(payload, AiCreditEvent.class);
        log.debug("Received credits refunded event workspaceId={} requestId={} reservationId={}", event.workspaceId(), event.creativeRequestId(), event.creditReservationId());
        hooks.onCreditsRefunded(event);
    }

    private <T> T convert(Object payload, Class<T> type) {
        return objectMapper.convertValue(payload, type);
    }
}
