package com.lebhas.creativesaas.generation.queue;

public interface GenerationJobQueuePublisher {

    void publish(GenerationJobQueuedEvent event);
}
