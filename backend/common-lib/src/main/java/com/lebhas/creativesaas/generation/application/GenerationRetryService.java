package com.lebhas.creativesaas.generation.application;

import com.lebhas.ai.cache.AiRetryThrottleService;
import com.lebhas.ai.cache.RetryThrottleState;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GenerationRetryService {

    private final AiRetryThrottleService retryThrottleService;

    public GenerationRetryService(AiRetryThrottleService retryThrottleService) {
        this.retryThrottleService = retryThrottleService;
    }

    public void validateRetryAllowed(UUID workspaceId, UUID creativeRequestId) {
        RetryThrottleState throttleState = retryThrottleService.recordRetry(workspaceId, creativeRequestId)
                .orElse(null);
        if (throttleState != null && !throttleState.allowed()) {
            throw new BusinessException(ErrorCode.GENERATION_RATE_LIMITED, "Creative request retry limit exceeded");
        }
    }

}
