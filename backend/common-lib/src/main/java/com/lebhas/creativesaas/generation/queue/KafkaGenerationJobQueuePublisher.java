package com.lebhas.creativesaas.generation.queue;

import com.lebhas.creativesaas.generation.event.GenerationEventProducer;
import com.lebhas.creativesaas.generation.event.GenerationJobQueuedEventDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@ConditionalOnExpression("'${platform.generation.queue.worker-enabled:false}' == 'true' && '${platform.generation.queue.provider:KAFKA}' == 'KAFKA'")
public class KafkaGenerationJobQueuePublisher implements GenerationJobQueuePublisher {

    private final GenerationEventProducer generationEventProducer;

    public KafkaGenerationJobQueuePublisher(GenerationEventProducer generationEventProducer) {
        this.generationEventProducer = generationEventProducer;
    }

    @Override
    public void publish(GenerationJobQueuedEvent event) {
        generationEventProducer.publishJobQueued(new GenerationJobQueuedEventDto(
                event.workspaceId(),
                event.requestId(),
                event.jobId(),
                null,
                event.queueName(),
                Instant.now()));
    }
}
