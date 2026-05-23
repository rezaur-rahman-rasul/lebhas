package com.lebhas.ai.consumer;

import com.lebhas.ai.event.AiCostEvent;
import com.lebhas.ai.event.AiCreditEvent;
import com.lebhas.ai.event.AiGenerationLifecycleEvent;
import com.lebhas.ai.event.AiLayerLifecycleEvent;
import com.lebhas.ai.event.AiLayerUpdatedEvent;
import com.lebhas.ai.event.AiPipelineUpdatedEvent;
import com.lebhas.ai.event.AiProviderUpdatedEvent;
import com.lebhas.ai.event.AiRoutingResolvedEvent;

public interface AiCreativePipelineEventHooks {

    default void onPipelineUpdated(AiPipelineUpdatedEvent event) {
    }

    default void onLayerUpdated(AiLayerUpdatedEvent event) {
    }

    default void onProviderUpdated(AiProviderUpdatedEvent event) {
    }

    default void onRoutingResolved(AiRoutingResolvedEvent event) {
    }

    default void onGenerationRequested(AiGenerationLifecycleEvent event) {
    }

    default void onGenerationStarted(AiGenerationLifecycleEvent event) {
    }

    default void onGenerationCompleted(AiGenerationLifecycleEvent event) {
    }

    default void onGenerationFailed(AiGenerationLifecycleEvent event) {
    }

    default void onLayerStarted(AiLayerLifecycleEvent event) {
    }

    default void onLayerCompleted(AiLayerLifecycleEvent event) {
    }

    default void onLayerFailed(AiLayerLifecycleEvent event) {
    }

    default void onLayerFallbackUsed(AiLayerLifecycleEvent event) {
    }

    default void onCostEstimated(AiCostEvent event) {
    }

    default void onCostFinalized(AiCostEvent event) {
    }

    default void onCreditsReserved(AiCreditEvent event) {
    }

    default void onCreditsFinalized(AiCreditEvent event) {
    }

    default void onCreditsRefunded(AiCreditEvent event) {
    }
}
