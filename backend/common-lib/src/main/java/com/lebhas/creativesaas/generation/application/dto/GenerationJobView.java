package com.lebhas.creativesaas.generation.application.dto;

import com.lebhas.creativesaas.generation.domain.CreativeGenerationStatus;
import com.lebhas.creativesaas.generation.domain.GenerationJobType;

import java.time.Instant;
import java.util.UUID;

public record GenerationJobView(
        UUID id,
        UUID workspaceId,
        UUID requestId,
        GenerationJobType jobType,
        CreativeGenerationStatus status,
        String providerJobId,
        int attemptCount,
        int maxAttempts,
        String queueName,
        Instant startedAt,
        Instant completedAt,
        Instant failedAt,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
}
