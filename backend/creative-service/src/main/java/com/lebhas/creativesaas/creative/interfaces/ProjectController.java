package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.campaign.application.ProjectCampaignService;
import com.lebhas.creativesaas.campaign.application.dto.ProjectCampaignView;
import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@Tag(name = "Projects")
@SecurityRequirement(name = "bearerAuth")
public class ProjectController {

    private final ProjectCampaignService projectCampaignService;

    public ProjectController(ProjectCampaignService projectCampaignService) {
        this.projectCampaignService = projectCampaignService;
    }

    @PostMapping("/{workspaceId}/projects")
    @PreAuthorize("hasAuthority('PROJECT_CREATE')")
    @Operation(summary = "Create a project or campaign")
    public ApiResponse<ProjectCampaignView> createProject(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateProjectCampaignRequest request
    ) {
        if (request.productServiceId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "productServiceId is required.");
        }
        return ApiResponse.success(projectCampaignService.createProjectCampaign(
                workspaceId,
                request.productServiceId(),
                request.name(),
                request.description(),
                request.campaignObjective(),
                request.targetPlatform(),
                request.campaignType()));
    }

    @PostMapping("/{workspaceId}/product-services/{productServiceId}/projects")
    @PreAuthorize("hasAuthority('PROJECT_CREATE')")
    @Operation(summary = "Create a project or campaign")
    public ApiResponse<ProjectCampaignView> createProjectUnderProductService(
            @PathVariable UUID workspaceId,
            @PathVariable UUID productServiceId,
            @Valid @RequestBody CreateProjectCampaignRequest request
    ) {
        return ApiResponse.success(projectCampaignService.createProjectCampaign(
                workspaceId,
                productServiceId,
                request.name(),
                request.description(),
                request.campaignObjective(),
                request.targetPlatform(),
                request.campaignType()));
    }

    @GetMapping("/{workspaceId}/projects")
    @PreAuthorize("hasAuthority('PROJECT_VIEW')")
    @Operation(summary = "List project campaigns in a workspace")
    public ApiResponse<List<ProjectCampaignView>> listProjects(@PathVariable UUID workspaceId) {
        return ApiResponse.success(projectCampaignService.listProjectCampaigns(workspaceId));
    }

    @GetMapping("/{workspaceId}/product-services/{productServiceId}/projects")
    @PreAuthorize("hasAuthority('PROJECT_VIEW')")
    @Operation(summary = "List project campaigns for a product or service")
    public ApiResponse<List<ProjectCampaignView>> listProjectsByProductService(
            @PathVariable UUID workspaceId,
            @PathVariable UUID productServiceId
    ) {
        return ApiResponse.success(projectCampaignService.listProjectCampaigns(workspaceId).stream()
                .filter(project -> productServiceId.equals(project.productServiceId()))
                .toList());
    }

    @GetMapping("/{workspaceId}/projects/{projectId}")
    @PreAuthorize("hasAuthority('PROJECT_VIEW')")
    @Operation(summary = "Get a project or campaign by id")
    public ApiResponse<ProjectCampaignView> getProject(
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId
    ) {
        return ApiResponse.success(projectCampaignService.getProjectCampaign(workspaceId, projectId));
    }

    @PutMapping("/{workspaceId}/projects/{projectId}")
    @PreAuthorize("hasAuthority('PROJECT_UPDATE')")
    @Operation(summary = "Update a project or campaign")
    public ApiResponse<ProjectCampaignView> updateProject(
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId,
            @Valid @RequestBody UpdateProjectCampaignRequest request
    ) {
        return ApiResponse.success(projectCampaignService.updateProjectCampaign(
                workspaceId,
                projectId,
                request.name(),
                request.description(),
                request.campaignObjective(),
                request.targetPlatform(),
                request.campaignType(),
                request.status()));
    }

    @DeleteMapping("/{workspaceId}/projects/{projectId}")
    @PreAuthorize("hasAuthority('PROJECT_UPDATE')")
    @Operation(summary = "Delete a project or campaign")
    public ApiResponse<Void> deleteProject(@PathVariable UUID workspaceId, @PathVariable UUID projectId) {
        projectCampaignService.deleteProjectCampaign(workspaceId, projectId);
        return ApiResponse.success("Project deleted", null);
    }
}
