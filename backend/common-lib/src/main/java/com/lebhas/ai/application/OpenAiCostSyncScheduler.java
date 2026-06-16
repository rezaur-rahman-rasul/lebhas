package com.lebhas.ai.application;

import org.springframework.scheduling.annotation.Scheduled;

public class OpenAiCostSyncScheduler {

    private final OpenAiCostTrackingService costTrackingService;

    public OpenAiCostSyncScheduler(OpenAiCostTrackingService costTrackingService) {
        this.costTrackingService = costTrackingService;
    }

    @Scheduled(fixedDelayString = "${lebhas.openai.cost-sync.fixed-delay-ms:60000}")
    public void syncOpenAiCosts() {
        costTrackingService.syncEnabledProviders();
    }
}
