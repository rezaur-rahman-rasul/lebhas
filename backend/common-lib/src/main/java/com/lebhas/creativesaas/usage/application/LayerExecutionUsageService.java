package com.lebhas.creativesaas.usage.application;

import com.lebhas.creativesaas.usage.application.dto.AiCostUsageView;
import com.lebhas.creativesaas.usage.application.dto.AiLayerUsageBillingCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LayerExecutionUsageService {

    private final AiLayerUsageBillingService aiLayerUsageBillingService;

    public LayerExecutionUsageService(AiLayerUsageBillingService aiLayerUsageBillingService) {
        this.aiLayerUsageBillingService = aiLayerUsageBillingService;
    }

    @Transactional
    public AiCostUsageView recordCompletedLayerExecution(AiLayerUsageBillingCommand command) {
        return aiLayerUsageBillingService.recordLayerExecutionUsage(command);
    }
}
