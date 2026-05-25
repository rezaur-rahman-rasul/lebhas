package com.lebhas.creativesaas.activity.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WorkspaceTimelineService {

    private final ActivityFeedService activityFeedService;

    public WorkspaceTimelineService(ActivityFeedService activityFeedService) {
        this.activityFeedService = activityFeedService;
    }

    @Transactional(readOnly = true)
    public List<ActivityFeedView> workspaceTimeline(UUID workspaceId, int limit) {
        return activityFeedService.listWorkspaceActivities(workspaceId, limit);
    }

    @Transactional(readOnly = true)
    public List<ActivityFeedView> referenceTimeline(UUID workspaceId, String referenceType, UUID referenceId, int limit) {
        return activityFeedService.listReferenceActivities(workspaceId, referenceType, referenceId, limit);
    }
}
