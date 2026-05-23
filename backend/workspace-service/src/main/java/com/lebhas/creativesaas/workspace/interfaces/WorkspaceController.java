package com.lebhas.creativesaas.workspace.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.workspace.application.WorkspaceManagementService;
import com.lebhas.creativesaas.workspace.application.dto.CreateWorkspaceCommand;
import com.lebhas.creativesaas.workspace.application.dto.UpdateWorkspaceCommand;
import com.lebhas.creativesaas.workspace.application.WorkspaceContextService;
import com.lebhas.creativesaas.workspace.application.dto.WorkspaceContextView;
import com.lebhas.creativesaas.workspace.application.dto.WorkspaceSummaryView;
import com.lebhas.creativesaas.workspace.application.dto.WorkspaceView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/workspaces")
@Tag(name = "Workspaces")
@SecurityRequirement(name = "bearerAuth")
public class WorkspaceController {

    private final WorkspaceManagementService workspaceManagementService;
    private final WorkspaceContextService workspaceContextService;

    public WorkspaceController(
            WorkspaceManagementService workspaceManagementService,
            WorkspaceContextService workspaceContextService
    ) {
        this.workspaceManagementService = workspaceManagementService;
        this.workspaceContextService = workspaceContextService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WORKSPACE_CREATE')")
    @Operation(summary = "Create a workspace")
    public ApiResponse<WorkspaceView> createWorkspace(@Valid @RequestBody CreateWorkspaceRequest request) {
        return ApiResponse.success(workspaceManagementService.createWorkspace(new CreateWorkspaceCommand(
                request.name(),
                request.slug(),
                request.logoUrl(),
                request.description(),
                request.industry(),
                request.timezone(),
                request.language(),
                request.currency(),
                request.country())));
    }

    @GetMapping("/me")
    @Operation(summary = "List workspaces accessible to the current user")
    public ApiResponse<List<WorkspaceSummaryView>> listMyWorkspaces() {
        return ApiResponse.success(workspaceManagementService.listAccessibleWorkspaces());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('WORKSPACE_VIEW')")
    @Operation(summary = "Get workspace details")
    public ApiResponse<WorkspaceView> getWorkspace(@PathVariable UUID id) {
        return ApiResponse.success(workspaceManagementService.getWorkspace(id));
    }

    @GetMapping("/{id}/context")
    @PreAuthorize("hasAuthority('WORKSPACE_VIEW')")
    @Operation(summary = "Get workspace context for the current user")
    public ApiResponse<WorkspaceContextView> getWorkspaceContext(@PathVariable UUID id) {
        return ApiResponse.success(workspaceContextService.getWorkspaceContext(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WORKSPACE_UPDATE')")
    @Operation(summary = "Update workspace details or status")
    public ApiResponse<WorkspaceView> updateWorkspace(@PathVariable UUID id, @Valid @RequestBody UpdateWorkspaceRequest request) {
        return ApiResponse.success(workspaceManagementService.updateWorkspace(new UpdateWorkspaceCommand(
                id,
                request.name(),
                request.slug(),
                request.logoUrl(),
                request.description(),
                request.industry(),
                request.timezone(),
                request.language(),
                request.currency(),
                request.country(),
                request.status())));
    }
}
