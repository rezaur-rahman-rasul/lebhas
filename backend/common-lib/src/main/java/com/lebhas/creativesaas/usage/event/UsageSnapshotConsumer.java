package com.lebhas.creativesaas.usage.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.creativesaas.usage.application.MonthlyUsageSnapshotService;
import com.lebhas.creativesaas.usage.application.dto.MonthlyUsageSnapshotView;
import com.lebhas.creativesaas.usage.cache.UsageMonthlyCounterService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class UsageSnapshotConsumer {

    private final ObjectMapper objectMapper;
    private final MonthlyUsageSnapshotService monthlyUsageSnapshotService;
    private final UsageMonthlyCounterService usageMonthlyCounterService;

    public UsageSnapshotConsumer(
            ObjectMapper objectMapper,
            MonthlyUsageSnapshotService monthlyUsageSnapshotService,
            UsageMonthlyCounterService usageMonthlyCounterService
    ) {
        this.objectMapper = objectMapper;
        this.monthlyUsageSnapshotService = monthlyUsageSnapshotService;
        this.usageMonthlyCounterService = usageMonthlyCounterService;
    }

    @KafkaListener(
            topics = "#{@usageBillingKafkaTopicNames.usageSnapshotCreated()}",
            groupId = "${platform.usage.kafka.consumer-group:${spring.application.name}-usage}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(Object payload) {
        UsageSnapshotCreatedEventDto event = objectMapper.convertValue(payload, UsageSnapshotCreatedEventDto.class);
        if (event == null || event.workspaceId() == null || event.usageMonth() == null) {
            return;
        }
        monthlyUsageSnapshotService.getSnapshot(event.workspaceId(), event.usageMonth())
                .ifPresent(this::cacheSnapshot);
    }

    private void cacheSnapshot(MonthlyUsageSnapshotView snapshot) {
        usageMonthlyCounterService.put(new UsageMonthlyCounterService.MonthlyUsageCounterSnapshot(
                snapshot.workspaceId(),
                snapshot.usageMonth(),
                snapshot.usedCredits(),
                snapshot.generatedVersions(),
                snapshot.creativeRequests(),
                snapshot.aiCostUsd(),
                snapshot.downloads(),
                snapshot.publicShares(),
                Instant.now()));
    }
}
