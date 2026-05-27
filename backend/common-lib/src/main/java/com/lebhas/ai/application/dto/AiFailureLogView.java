package com.lebhas.ai.application.dto;

import com.lebhas.ai.domain.AiFailureType;
import com.lebhas.creativesaas.profile.application.dto.SafeProfileDisplayView;

import java.time.Instant;
import java.util.UUID;

public record AiFailureLogView(
        UUID id,
        UUID creativeRequestId,
        SafeProfileDisplayView actorDisplay,
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
