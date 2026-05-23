package com.lebhas.ai.application.dto;

import com.lebhas.ai.domain.AiFailureType;

import java.time.Instant;
import java.util.UUID;

public record AiFailureLogView(
        UUID id,
        UUID creativeRequestId,
        UUID layerId,
        UUID providerId,
        String modelName,
        AiFailureType failureType,
        String failureReason,
        int retryAttempt,
        boolean fallbackTriggered,
        Instant createdAt
) {
}
