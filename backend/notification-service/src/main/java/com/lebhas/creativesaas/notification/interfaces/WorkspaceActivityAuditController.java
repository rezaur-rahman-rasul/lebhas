package com.lebhas.creativesaas.notification.interfaces;

import com.lebhas.creativesaas.activity.application.ActivityFeedService;
import com.lebhas.creativesaas.activity.application.ActivityFeedView;
import com.lebhas.creativesaas.activity.application.WorkspaceTimelineService;
import com.lebhas.creativesaas.auditlog.application.AuditLogView;
import com.lebhas.creativesaas.auditlog.application.AuditQueryService;
import com.lebhas.creativesaas.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}")
@Tag(name = "Workspace Activity")
@SecurityRequirement(name = "bearerAuth")
public class WorkspaceActivityAuditController {

    private final ActivityFeedService activityFeedService;
    private final WorkspaceTimelineService workspaceTimelineService;
    private final AuditQueryService auditQueryService;

    public WorkspaceActivityAuditController(
            ActivityFeedService activityFeedService,
            WorkspaceTimelineService workspaceTimelineService,
            AuditQueryService auditQueryService
    ) {
        this.activityFeedService = activityFeedService;
        this.workspaceTimelineService = workspaceTimelineService;
        this.auditQueryService = auditQueryService;
    }

    @GetMapping("/activity-feed")
    @Operation(summary = "List workspace activity feed")
    public ApiResponse<List<ActivityFeedView>> activityFeed(
            @PathVariable UUID workspaceId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ApiResponse.success(activityFeedService.listWorkspaceActivities(workspaceId, limit));
    }

    @GetMapping("/timeline")
    @Operation(summary = "List workspace timeline")
    public ApiResponse<List<ActivityFeedView>> timeline(
            @PathVariable UUID workspaceId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ApiResponse.success(workspaceTimelineService.workspaceTimeline(workspaceId, limit));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "List workspace audit logs")
    public ApiResponse<List<AuditLogView>> auditLogs(
            @PathVariable UUID workspaceId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ApiResponse.success(auditQueryService.listWorkspaceAuditLogs(workspaceId, limit));
    }
}
