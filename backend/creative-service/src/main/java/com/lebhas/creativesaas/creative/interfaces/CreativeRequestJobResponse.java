package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.ai.job.AiJobState;
import com.lebhas.ai.provider.AiProviderType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "AI job state associated with a creative request.")
public record CreativeRequestJobResponse(
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
