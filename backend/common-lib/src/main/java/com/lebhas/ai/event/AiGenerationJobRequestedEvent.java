package com.lebhas.ai.event;

import com.lebhas.ai.provider.AiProviderType;

import java.time.Instant;
import java.util.UUID;

public record AiGenerationJobRequestedEvent(
        UUID workspaceId,
        UUID creativeRequestId,
        UUID jobId,
        AiProviderType providerType,
        String model,
        Instant requestedAt
) {
}
