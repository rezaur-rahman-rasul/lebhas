package com.lebhas.creativesaas.profile.event;

import com.lebhas.creativesaas.profile.domain.UserSecurityActivityType;

import java.time.Instant;
import java.util.UUID;

public record ProfileSecurityActivityCreatedEventDto(
        UUID workspaceId,
        UUID securityActivityId,
        UUID userId,
        UUID actorUserId,
        UserSecurityActivityType activityType,
        boolean success,
        String failureReason,
        Instant occurredAt
) {
}
