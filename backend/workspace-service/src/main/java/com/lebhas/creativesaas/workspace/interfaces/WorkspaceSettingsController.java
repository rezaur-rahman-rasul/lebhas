package com.lebhas.creativesaas.workspace.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.workspace.application.WorkspaceSettingsService;
import com.lebhas.creativesaas.workspace.application.dto.NotificationPreferencesView;
import com.lebhas.creativesaas.workspace.application.dto.UpdateWorkspaceSettingsCommand;
import com.lebhas.creativesaas.workspace.application.dto.WorkspaceSettingsView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{id}/settings")
@Tag(name = "Workspace Settings")
@SecurityRequirement(name = "bearerAuth")
public class WorkspaceSettingsController {

    private final WorkspaceSettingsService workspaceSettingsService;

    public WorkspaceSettingsController(WorkspaceSettingsService workspaceSettingsService) {
        this.workspaceSettingsService = workspaceSettingsService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('WORKSPACE_SETTINGS_VIEW')")
    @Operation(summary = "Get workspace settings")
    public ApiResponse<WorkspaceSettingsView> getSettings(@PathVariable UUID id) {
        return ApiResponse.success(workspaceSettingsService.getSettings(id));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('WORKSPACE_SETTINGS_UPDATE')")
    @Operation(summary = "Update workspace settings")
    public ApiResponse<WorkspaceSettingsView> updateSettings(@PathVariable UUID id, @Valid @RequestBody UpdateWorkspaceSettingsRequest request) {
        return ApiResponse.success(workspaceSettingsService.updateSettings(new UpdateWorkspaceSettingsCommand(
                id,
                request.allowCrewDownload(),
                request.allowCrewPublish(),
                request.defaultLanguage(),
                request.defaultTimezone(),
                new NotificationPreferencesView(
                        request.notificationPreferences().crewInvites(),
                        request.notificationPreferences().workspaceUpdates(),
                        request.notificationPreferences().securityAlerts()),
                request.workspaceVisibility())));
    }
}
