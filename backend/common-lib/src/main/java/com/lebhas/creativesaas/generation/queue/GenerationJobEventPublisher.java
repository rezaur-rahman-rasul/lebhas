package com.lebhas.creativesaas.generation.queue;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class GenerationJobEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public GenerationJobEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publish(GenerationJobQueuedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
