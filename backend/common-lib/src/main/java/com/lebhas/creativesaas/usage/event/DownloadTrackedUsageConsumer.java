package com.lebhas.creativesaas.usage.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.creativesaas.usage.application.UsageSummaryMapper;
import com.lebhas.creativesaas.usage.cache.DownloadCounterCacheService;
import com.lebhas.creativesaas.usage.cache.WorkspaceUsageSummaryCacheService;
import com.lebhas.creativesaas.usage.domain.WorkspaceUsageSummary;
import com.lebhas.creativesaas.usage.infrastructure.persistence.WorkspaceUsageSummaryRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Component
public class DownloadTrackedUsageConsumer {

    private final ObjectMapper objectMapper;
    private final WorkspaceUsageSummaryRepository summaryRepository;
    private final UsageSummaryMapper usageSummaryMapper;
    private final WorkspaceUsageSummaryCacheService summaryCacheService;
    private final DownloadCounterCacheService downloadCounterCacheService;
    private final UsageBillingEventProducer eventProducer;

    public DownloadTrackedUsageConsumer(
            ObjectMapper objectMapper,
            WorkspaceUsageSummaryRepository summaryRepository,
            UsageSummaryMapper usageSummaryMapper,
            WorkspaceUsageSummaryCacheService summaryCacheService,
            DownloadCounterCacheService downloadCounterCacheService,
            UsageBillingEventProducer eventProducer
    ) {
        this.objectMapper = objectMapper;
        this.summaryRepository = summaryRepository;
        this.usageSummaryMapper = usageSummaryMapper;
        this.summaryCacheService = summaryCacheService;
        this.downloadCounterCacheService = downloadCounterCacheService;
        this.eventProducer = eventProducer;
    }

    @KafkaListener(
            topics = "#{@usageBillingKafkaTopicNames.downloadTracked()}",
            groupId = "${platform.usage.kafka.consumer-group:${spring.application.name}-usage}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(Object payload) {
        DownloadTrackedEventDto event = objectMapper.convertValue(payload, DownloadTrackedEventDto.class);
        if (event != null && event.workspaceId() != null) {
            updateUsage(event);
        }
    }

    @Transactional
    protected void updateUsage(DownloadTrackedEventDto event) {
        LocalDate month = normalizeMonth(event.usageMonth());
        WorkspaceUsageSummary summary = summaryRepository.findByWorkspaceIdAndUsageMonth(event.workspaceId(), month)
                .orElseGet(() -> summaryRepository.save(WorkspaceUsageSummary.create(event.workspaceId(), month)));
        if (!event.summaryUpdated()) {
            summary.recordDownload();
        }
        summaryCacheService.put(usageSummaryMapper.toView(summary));
        downloadCounterCacheService.put(event.workspaceId(), month, summary.getTotalDownloads());
        eventProducer.publishUsageUpdated(new UsageUpdatedEventDto(
                event.workspaceId(),
                month,
                event.downloadUsageLogId(),
                "DOWNLOAD_USAGE_LOG",
                "DOWNLOAD_TRACKED",
                event.occurredAt()));
    }

    private LocalDate normalizeMonth(LocalDate month) {
        return (month == null ? LocalDate.now(ZoneOffset.UTC) : month).withDayOfMonth(1);
    }
}
