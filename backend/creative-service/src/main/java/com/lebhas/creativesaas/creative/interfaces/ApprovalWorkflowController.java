package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.approval.application.ApprovalActionService;
import com.lebhas.creativesaas.approval.application.ApprovalWorkflowService;
import com.lebhas.creativesaas.approval.application.dto.ApprovalActionCommand;
import com.lebhas.creativesaas.approval.application.dto.ApprovalWorkflowView;
import com.lebhas.creativesaas.approval.application.dto.CreateApprovalWorkflowCommand;
import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.common.constants.CommonHeaders;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/approval-workflows")
@Tag(name = "Approval Workflows")
@SecurityRequirement(name = "bearerAuth")
public class ApprovalWorkflowController {

    private final ApprovalWorkflowService approvalWorkflowService;
    private final ApprovalActionService approvalActionService;
    private final WorkspaceRequestResolver workspaceRequestResolver;

    public ApprovalWorkflowController(
            ApprovalWorkflowService approvalWorkflowService,
            ApprovalActionService approvalActionService,
            WorkspaceRequestResolver workspaceRequestResolver
    ) {
        this.approvalWorkflowService = approvalWorkflowService;
        this.approvalActionService = approvalActionService;
        this.workspaceRequestResolver = workspaceRequestResolver;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('CREATIVE_SUBMIT','GENERATED_VERSION_MANAGE')")
    @Operation(summary = "Create an approval workflow")
    @Parameter(
            name = CommonHeaders.WORKSPACE_ID,
            in = ParameterIn.HEADER,
            required = false,
            description = "Workspace context. Optional when the access token is already bound to a workspace; required for master or cross-workspace access.",
            schema = @Schema(type = "string", format = "uuid"))
    public ApiResponse<ApprovalWorkflowView> createApprovalWorkflow(
            @Valid @RequestBody CreateApprovalWorkflowRequest request
    ) {
        UUID workspaceId = workspaceRequestResolver.requireWorkspaceId();
        ApprovalWorkflowView workflow = approvalWorkflowService.createApprovalWorkflow(
                new CreateApprovalWorkflowCommand(
                        workspaceId,
                        request.creativeRequestId(),
                        request.generatedVersionId(),
                        request.currentReviewerId()));
        return ApiResponse.success("Approval workflow created", workflow);
    }

    @GetMapping("/{approvalWorkflowId}")
    @PreAuthorize("hasAnyAuthority('CREATIVE_SUBMIT','GENERATED_VERSION_MANAGE','CREATIVE_DOWNLOAD','SUPPORT_WORKSPACE_ACCESS')")
    @Operation(summary = "Get an approval workflow")
    @Parameter(
            name = CommonHeaders.WORKSPACE_ID,
            in = ParameterIn.HEADER,
            required = false,
            description = "Workspace context. Optional when the access token is already bound to a workspace; required for master or cross-workspace access.",
            schema = @Schema(type = "string", format = "uuid"))
    public ApiResponse<ApprovalWorkflowView> getApprovalWorkflow(@PathVariable UUID approvalWorkflowId) {
        UUID workspaceId = workspaceRequestResolver.requireWorkspaceId();
        return ApiResponse.success(
                "Approval workflow found",
                approvalWorkflowService.getApprovalWorkflow(workspaceId, approvalWorkflowId));
    }

    @PostMapping("/{approvalWorkflowId}/approve")
    @PreAuthorize("hasAnyAuthority('CREATIVE_SUBMIT','GENERATED_VERSION_MANAGE')")
    @Operation(summary = "Approve an approval workflow")
    @Parameter(
            name = CommonHeaders.WORKSPACE_ID,
            in = ParameterIn.HEADER,
            required = false,
            description = "Workspace context. Optional when the access token is already bound to a workspace; required for master or cross-workspace access.",
            schema = @Schema(type = "string", format = "uuid"))
    public ApiResponse<ApprovalWorkflowView> approve(
            @PathVariable UUID approvalWorkflowId,
            @Valid @RequestBody(required = false) ApprovalActionRequest request
    ) {
        UUID workspaceId = workspaceRequestResolver.requireWorkspaceId();
        return ApiResponse.success(
                "Approval workflow approved",
                approvalActionService.approve(toActionCommand(workspaceId, approvalWorkflowId, request)));
    }

    @PostMapping("/{approvalWorkflowId}/reject")
    @PreAuthorize("hasAnyAuthority('CREATIVE_SUBMIT','GENERATED_VERSION_MANAGE')")
    @Operation(summary = "Reject an approval workflow")
    @Parameter(
            name = CommonHeaders.WORKSPACE_ID,
            in = ParameterIn.HEADER,
            required = false,
            description = "Workspace context. Optional when the access token is already bound to a workspace; required for master or cross-workspace access.",
            schema = @Schema(type = "string", format = "uuid"))
    public ApiResponse<ApprovalWorkflowView> reject(
            @PathVariable UUID approvalWorkflowId,
            @Valid @RequestBody(required = false) ApprovalActionRequest request
    ) {
        UUID workspaceId = workspaceRequestResolver.requireWorkspaceId();
        return ApiResponse.success(
                "Approval workflow rejected",
                approvalActionService.reject(toActionCommand(workspaceId, approvalWorkflowId, request)));
    }

    @PostMapping("/{approvalWorkflowId}/request-revision")
    @PreAuthorize("hasAnyAuthority('CREATIVE_SUBMIT','GENERATED_VERSION_MANAGE')")
    @Operation(summary = "Request a revision for an approval workflow")
    @Parameter(
            name = CommonHeaders.WORKSPACE_ID,
            in = ParameterIn.HEADER,
            required = false,
            description = "Workspace context. Optional when the access token is already bound to a workspace; required for master or cross-workspace access.",
            schema = @Schema(type = "string", format = "uuid"))
    public ApiResponse<ApprovalWorkflowView> requestRevision(
            @PathVariable UUID approvalWorkflowId,
            @Valid @RequestBody(required = false) ApprovalActionRequest request
    ) {
        UUID workspaceId = workspaceRequestResolver.requireWorkspaceId();
        return ApiResponse.success(
                "Approval workflow revision requested",
                approvalActionService.requestRevision(toActionCommand(workspaceId, approvalWorkflowId, request)));
    }

    private ApprovalActionCommand toActionCommand(
            UUID workspaceId,
            UUID approvalWorkflowId,
            ApprovalActionRequest request
    ) {
        return new ApprovalActionCommand(
                workspaceId,
                approvalWorkflowId,
                request == null ? null : request.comments());
    }

}
