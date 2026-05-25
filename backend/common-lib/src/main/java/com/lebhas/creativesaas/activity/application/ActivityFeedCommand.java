package com.lebhas.creativesaas.activity.application;

import com.lebhas.creativesaas.activity.domain.ActivityCategory;

import java.time.Instant;
import java.util.UUID;

public record ActivityFeedCommand(
        UUID workspaceId,
        String sourceEventId,
        UUID actorUserId,
        ActivityCategory activityCategory,
        String activityType,
        String title,
        String description,
        String referenceType,
        UUID referenceId,
        Instant activityAt
) {
}
