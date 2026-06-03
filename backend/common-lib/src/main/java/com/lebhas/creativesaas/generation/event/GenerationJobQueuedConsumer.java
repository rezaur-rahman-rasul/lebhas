package com.lebhas.creativesaas.generation.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.creativesaas.generation.application.GenerationWorkerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "platform.generation.kafka", name = "consumer-enabled", havingValue = "true", matchIfMissing = true)
public class GenerationJobQueuedConsumer {

    private static final Logger log = LoggerFactory.getLogger(GenerationJobQueuedConsumer.class);

    private final ObjectMapper objectMapper;
    private final GenerationWorkerService generationWorkerService;

    public GenerationJobQueuedConsumer(
            ObjectMapper objectMapper,
            GenerationWorkerService generationWorkerService
    ) {
        this.objectMapper = objectMapper;
        this.generationWorkerService = generationWorkerService;
    }

    @KafkaListener(
            topics = "#{@creativeGenerationKafkaTopicNames.generationJobQueued()}",
            groupId = "${platform.generation.kafka.consumer-group:${spring.application.name}-generation}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(Object payload) {
        GenerationJobQueuedEventDto event = toEvent(payload);
        if (event == null) {
            return;
        }
        try {
            generationWorkerService.processQueuedJob(event);
        } catch (RuntimeException exception) {
            log.warn("generation_event type=queued_job_consume_failed workspaceId={} requestId={} jobId={} reason={}",
                    event.workspaceId(),
                    event.creativeRequestId(),
                    event.generationJobId(),
                    safeReason(exception));
        }
    }

    private GenerationJobQueuedEventDto toEvent(Object payload) {
        try {
            GenerationJobQueuedEventDto event = objectMapper.convertValue(payload, GenerationJobQueuedEventDto.class);
            if (event.workspaceId() == null || event.creativeRequestId() == null || event.generationJobId() == null) {
                return null;
            }
            return event;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String safeReason(Throwable exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        String normalized = message.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 1000 ? normalized : normalized.substring(0, 1000);
    }
}
