package com.lebhas.creativesaas.creativerequest.application.dto;

import com.lebhas.ai.job.AiJobState;
import com.lebhas.ai.provider.AiProviderType;

import java.time.Instant;
import java.util.UUID;

public record CreativeRequestJobView(
        UUID jobId,
        AiProviderType providerType,
        String model,
        AiJobState state,
        int attempt,
        String providerJobId,
        String message,
        Instant updatedAt
) {
}
