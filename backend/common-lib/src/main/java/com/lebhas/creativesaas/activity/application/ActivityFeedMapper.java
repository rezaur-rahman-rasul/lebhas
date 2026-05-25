package com.lebhas.creativesaas.activity.application;

import com.lebhas.creativesaas.activity.domain.ActivityFeed;
import org.springframework.stereotype.Component;

@Component
public class ActivityFeedMapper {

    public ActivityFeedView toView(ActivityFeed activityFeed) {
        return new ActivityFeedView(
                activityFeed.getId(),
                activityFeed.getWorkspaceId(),
                activityFeed.getSourceEventId(),
                activityFeed.getActorUserId(),
                activityFeed.getActivityCategory(),
                activityFeed.getActivityType(),
                activityFeed.getTitle(),
                activityFeed.getDescription(),
                activityFeed.getReferenceType(),
                activityFeed.getReferenceId(),
                activityFeed.getActivityAt(),
                activityFeed.getCreatedAt());
    }
}
