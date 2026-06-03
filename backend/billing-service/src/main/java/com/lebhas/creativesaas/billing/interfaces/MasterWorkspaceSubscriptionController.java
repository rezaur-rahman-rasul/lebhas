package com.lebhas.creativesaas.billing.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.pricing.application.WorkspacePlanContextService;
import com.lebhas.creativesaas.pricing.application.WorkspaceSubscriptionService;
import com.lebhas.creativesaas.pricing.application.dto.AssignWorkspaceSubscriptionCommand;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/master/workspaces")
@Tag(name = "Master Pricing")
@SecurityRequirement(name = "bearerAuth")
public class MasterWorkspaceSubscriptionController {

    private final WorkspaceSubscriptionService workspaceSubscriptionService;
    private final WorkspacePlanContextService workspacePlanContextService;
    private final PricingApiMapper pricingApiMapper;

    public MasterWorkspaceSubscriptionController(
            WorkspaceSubscriptionService workspaceSubscriptionService,
            WorkspacePlanContextService workspacePlanContextService,
            PricingApiMapper pricingApiMapper
    ) {
        this.workspaceSubscriptionService = workspaceSubscriptionService;
        this.workspacePlanContextService = workspacePlanContextService;
        this.pricingApiMapper = pricingApiMapper;
    }

    @PostMapping("/{workspaceId}/subscription")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Assign or change a workspace subscription")
    public ApiResponse<WorkspacePlanContextResponse> assignWorkspaceSubscription(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody AssignWorkspaceSubscriptionRequest request
    ) {
        workspaceSubscriptionService.assignOrChangeWorkspaceSubscription(new AssignWorkspaceSubscriptionCommand(
                workspaceId,
                request.pricingPlanId(),
                request.status(),
                request.startedAt(),
                request.expiresAt(),
                request.trialEndsAt(),
                request.autoRenew()));
        return ApiResponse.success(pricingApiMapper.toWorkspacePlanContextResponse(
                workspacePlanContextService.getWorkspacePlanContextForMaster(workspaceId)));
    }

    @GetMapping("/{workspaceId}/subscription")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Get workspace subscription and active plan context")
    public ApiResponse<WorkspacePlanContextResponse> getWorkspaceSubscription(@PathVariable UUID workspaceId) {
        return ApiResponse.success(pricingApiMapper.toWorkspacePlanContextResponse(
                workspacePlanContextService.getWorkspacePlanContextForMaster(workspaceId)));
    }
}
