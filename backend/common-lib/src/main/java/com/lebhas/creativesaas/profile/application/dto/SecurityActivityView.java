package com.lebhas.creativesaas.profile.application.dto;

import com.lebhas.creativesaas.profile.domain.UserSecurityActivityType;

import java.time.Instant;
import java.util.UUID;

public record SecurityActivityView(
        UUID id,
        UUID userId,
        UserSecurityActivityType activityType,
        String ipAddress,
        String userAgent,
        String locationHint,
        boolean success,
        String failureReason,
        Instant createdAt
) {
}
