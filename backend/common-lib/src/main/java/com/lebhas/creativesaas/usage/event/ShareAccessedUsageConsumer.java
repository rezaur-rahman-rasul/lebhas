package com.lebhas.creativesaas.usage.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.creativesaas.usage.application.UsageSummaryMapper;
import com.lebhas.creativesaas.usage.cache.ShareCounterCacheService;
import com.lebhas.creativesaas.usage.cache.WorkspaceUsageSummaryCacheService;
import com.lebhas.creativesaas.usage.domain.WorkspaceUsageSummary;
import com.lebhas.creativesaas.usage.infrastructure.persistence.WorkspaceUsageSummaryRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Component
public class ShareAccessedUsageConsumer {

    private final ObjectMapper objectMapper;
    private final WorkspaceUsageSummaryRepository summaryRepository;
    private final UsageSummaryMapper usageSummaryMapper;
    private final WorkspaceUsageSummaryCacheService summaryCacheService;
    private final ShareCounterCacheService shareCounterCacheService;
    private final UsageBillingEventProducer eventProducer;

    public ShareAccessedUsageConsumer(
            ObjectMapper objectMapper,
            WorkspaceUsageSummaryRepository summaryRepository,
            UsageSummaryMapper usageSummaryMapper,
            WorkspaceUsageSummaryCacheService summaryCacheService,
            ShareCounterCacheService shareCounterCacheService,
            UsageBillingEventProducer eventProducer
    ) {
        this.objectMapper = objectMapper;
        this.summaryRepository = summaryRepository;
        this.usageSummaryMapper = usageSummaryMapper;
        this.summaryCacheService = summaryCacheService;
        this.shareCounterCacheService = shareCounterCacheService;
        this.eventProducer = eventProducer;
    }

    @KafkaListener(
            topics = "#{@usageBillingKafkaTopicNames.shareAccessed()}",
            groupId = "${platform.usage.kafka.consumer-group:${spring.application.name}-usage}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(Object payload) {
        ShareAccessedEventDto event = objectMapper.convertValue(payload, ShareAccessedEventDto.class);
        if (event != null && event.workspaceId() != null) {
            updateUsage(event);
        }
    }

    @Transactional
    protected void updateUsage(ShareAccessedEventDto event) {
        LocalDate month = normalizeMonth(event.usageMonth());
        WorkspaceUsageSummary summary = summaryRepository.findByWorkspaceIdAndUsageMonth(event.workspaceId(), month)
                .orElseGet(() -> summaryRepository.save(WorkspaceUsageSummary.create(event.workspaceId(), month)));
        if (!event.summaryUpdated()) {
            summary.recordPublicShareAccess();
        }
        summaryCacheService.put(usageSummaryMapper.toView(summary));
        shareCounterCacheService.put(event.workspaceId(), month, summary.getTotalPublicShares());
        eventProducer.publishUsageUpdated(new UsageUpdatedEventDto(
                event.workspaceId(),
                month,
                event.shareUsageLogId(),
                "SHARE_USAGE_LOG",
                "SHARE_ACCESSED",
                event.occurredAt()));
    }

    private LocalDate normalizeMonth(LocalDate month) {
        return (month == null ? LocalDate.now(ZoneOffset.UTC) : month).withDayOfMonth(1);
    }
}
