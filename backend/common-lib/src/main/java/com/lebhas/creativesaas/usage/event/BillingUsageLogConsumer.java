package com.lebhas.creativesaas.usage.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.creativesaas.usage.cache.AiLayerCostCacheService;
import com.lebhas.creativesaas.usage.cache.BillingDashboardCacheService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Component
public class BillingUsageLogConsumer {

    private final ObjectMapper objectMapper;
    private final BillingDashboardCacheService billingDashboardCacheService;
    private final AiLayerCostCacheService aiLayerCostCacheService;
    private final UsageBillingEventProducer eventProducer;

    public BillingUsageLogConsumer(
            ObjectMapper objectMapper,
            BillingDashboardCacheService billingDashboardCacheService,
            AiLayerCostCacheService aiLayerCostCacheService,
            UsageBillingEventProducer eventProducer
    ) {
        this.objectMapper = objectMapper;
        this.billingDashboardCacheService = billingDashboardCacheService;
        this.aiLayerCostCacheService = aiLayerCostCacheService;
        this.eventProducer = eventProducer;
    }

    @KafkaListener(
            topics = "#{@usageBillingKafkaTopicNames.billingUsageLogged()}",
            groupId = "${platform.usage.kafka.consumer-group:${spring.application.name}-usage}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(Object payload) {
        BillingUsageLoggedEventDto event = objectMapper.convertValue(payload, BillingUsageLoggedEventDto.class);
        if (event == null || event.workspaceId() == null) {
            return;
        }
        billingDashboardCacheService.invalidate(event.workspaceId());
        aiLayerCostCacheService.invalidate(event.workspaceId(), month());
        eventProducer.publishUsageUpdated(new UsageUpdatedEventDto(
                event.workspaceId(),
                month(),
                event.usageBillingLogId(),
                "USAGE_BILLING_LOG",
                "BILLING_USAGE_LOGGED",
                event.occurredAt()));
    }

    private LocalDate month() {
        return LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
    }
}
