package com.lebhas.creativesaas.usage.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.creativesaas.generation.event.GenerationFailedEventDto;
import com.lebhas.creativesaas.usage.application.UsageSummaryMapper;
import com.lebhas.creativesaas.usage.cache.WorkspaceUsageSummaryCacheService;
import com.lebhas.creativesaas.usage.domain.WorkspaceUsageSummary;
import com.lebhas.creativesaas.usage.infrastructure.persistence.WorkspaceUsageSummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Component
public class GenerationFailedUsageConsumer {

    private static final Logger log = LoggerFactory.getLogger(GenerationFailedUsageConsumer.class);

    private final ObjectMapper objectMapper;
    private final WorkspaceUsageSummaryRepository summaryRepository;
    private final UsageSummaryMapper usageSummaryMapper;
    private final WorkspaceUsageSummaryCacheService summaryCacheService;
    private final UsageBillingEventProducer eventProducer;

    public GenerationFailedUsageConsumer(
            ObjectMapper objectMapper,
            WorkspaceUsageSummaryRepository summaryRepository,
            UsageSummaryMapper usageSummaryMapper,
            WorkspaceUsageSummaryCacheService summaryCacheService,
            UsageBillingEventProducer eventProducer
    ) {
        this.objectMapper = objectMapper;
        this.summaryRepository = summaryRepository;
        this.usageSummaryMapper = usageSummaryMapper;
        this.summaryCacheService = summaryCacheService;
        this.eventProducer = eventProducer;
    }

    @KafkaListener(
            topics = "#{@creativeGenerationKafkaTopicNames.generationFailed()}",
            groupId = "${platform.usage.kafka.consumer-group:${spring.application.name}-usage}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(Object payload) {
        GenerationFailedEventDto event = objectMapper.convertValue(payload, GenerationFailedEventDto.class);
        if (event == null || event.workspaceId() == null || !event.finalized()) {
            return;
        }
        updateUsage(event);
    }

    @Transactional
    protected void updateUsage(GenerationFailedEventDto event) {
        try {
            LocalDate month = month();
            WorkspaceUsageSummary summary = summaryRepository.findByWorkspaceIdAndUsageMonth(event.workspaceId(), month)
                    .orElseGet(() -> summaryRepository.save(WorkspaceUsageSummary.create(event.workspaceId(), month)));
            summary.recordGenerationFailureUsage();
            summaryCacheService.put(usageSummaryMapper.toView(summary));
            eventProducer.publishUsageUpdated(new UsageUpdatedEventDto(
                    event.workspaceId(),
                    month,
                    event.generatedVersionId() == null ? event.creativeRequestId() : event.generatedVersionId(),
                    "GENERATION_FAILED",
                    "GENERATION_FAILED",
                    event.occurredAt()));
        } catch (RuntimeException exception) {
            log.warn("usage_event type=generation_failed_update_failed workspaceId={} requestId={} reason={}",
                    event.workspaceId(), event.creativeRequestId(), exception.getMessage());
        }
    }

    private LocalDate month() {
        return LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
    }
}
