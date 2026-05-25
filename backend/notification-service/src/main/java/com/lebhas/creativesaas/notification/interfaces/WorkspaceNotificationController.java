package com.lebhas.creativesaas.notification.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.notification.NotificationPreferenceCommand;
import com.lebhas.notification.NotificationPreferenceService;
import com.lebhas.notification.NotificationPreferenceView;
import com.lebhas.notification.NotificationService;
import com.lebhas.notification.NotificationView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}")
@Tag(name = "Workspace Notifications")
@SecurityRequirement(name = "bearerAuth")
public class WorkspaceNotificationController {

    private final NotificationService notificationService;
    private final NotificationPreferenceService notificationPreferenceService;
    private final CurrentUserContext currentUserContext;

    public WorkspaceNotificationController(
            NotificationService notificationService,
            NotificationPreferenceService notificationPreferenceService,
            CurrentUserContext currentUserContext
    ) {
        this.notificationService = notificationService;
        this.notificationPreferenceService = notificationPreferenceService;
        this.currentUserContext = currentUserContext;
    }

    @GetMapping("/notifications")
    @Operation(summary = "List current user notifications")
    public ApiResponse<List<NotificationView>> notifications(@PathVariable UUID workspaceId) {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        return ApiResponse.success(notificationService.listUserNotifications(workspaceId, currentUser.userId()));
    }

    @PostMapping("/notifications/{notificationId}/read")
    @Operation(summary = "Mark a notification as read")
    public ApiResponse<NotificationView> markRead(
            @PathVariable UUID workspaceId,
            @PathVariable UUID notificationId
    ) {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        return ApiResponse.success(notificationService.markAsRead(workspaceId, notificationId, currentUser.userId()));
    }

    @PostMapping("/notifications/read-all")
    @Operation(summary = "Mark all current user notifications as read")
    public ApiResponse<List<NotificationView>> markAllRead(@PathVariable UUID workspaceId) {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        return ApiResponse.success(notificationService.markAllAsRead(workspaceId, currentUser.userId()));
    }

    @GetMapping("/notifications/unread-count")
    @Operation(summary = "Get current user unread notification count")
    public ApiResponse<UnreadNotificationCountResponse> unreadCount(@PathVariable UUID workspaceId) {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        return ApiResponse.success(new UnreadNotificationCountResponse(
                notificationService.unreadCount(workspaceId, currentUser.userId())));
    }

    @GetMapping("/notification-preferences")
    @Operation(summary = "List current user notification preferences")
    public ApiResponse<List<NotificationPreferenceView>> notificationPreferences(@PathVariable UUID workspaceId) {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        return ApiResponse.success(notificationPreferenceService.listUserPreferences(workspaceId, currentUser.userId()));
    }

    @PutMapping("/notification-preferences")
    @Operation(summary = "Update current user notification preferences")
    public ApiResponse<List<NotificationPreferenceView>> updateNotificationPreferences(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody NotificationPreferenceUpdateRequest request
    ) {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        List<NotificationPreferenceCommand> commands = request.preferences()
                .stream()
                .map(preference -> new NotificationPreferenceCommand(
                        workspaceId,
                        currentUser.userId(),
                        preference.notificationType(),
                        preference.inAppEnabled(),
                        preference.emailEnabled(),
                        preference.smsEnabled(),
                        preference.pushEnabled()))
                .toList();
        return ApiResponse.success(notificationPreferenceService.upsertAll(commands));
    }

    public record UnreadNotificationCountResponse(long unreadCount) {
    }
}
