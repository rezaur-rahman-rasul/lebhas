package com.lebhas.creativesaas.workspace.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.workspace.application.WorkspaceManagementService;
import com.lebhas.creativesaas.workspace.application.dto.WorkspaceSummaryView;
import com.lebhas.creativesaas.workspace.application.dto.WorkspaceView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/master/workspaces")
@Tag(name = "Master Workspaces")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('MASTER')")
public class MasterWorkspaceController {

    private final WorkspaceManagementService workspaceManagementService;

    public MasterWorkspaceController(WorkspaceManagementService workspaceManagementService) {
        this.workspaceManagementService = workspaceManagementService;
    }

    @GetMapping
    @Operation(summary = "List workspaces")
    public ApiResponse<List<WorkspaceSummaryView>> listWorkspaces() {
        return ApiResponse.success(workspaceManagementService.listAccessibleWorkspaces());
    }

    @GetMapping("/{workspaceId}")
    @Operation(summary = "Get workspace")
    public ApiResponse<WorkspaceView> getWorkspace(@PathVariable UUID workspaceId) {
        return ApiResponse.success(workspaceManagementService.getWorkspace(workspaceId));
    }
}
