package com.lebhas.creativesaas.billing.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.pricing.application.WorkspacePlanContextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}")
@Tag(name = "Workspace Subscription")
@SecurityRequirement(name = "bearerAuth")
public class WorkspaceSubscriptionController {

    private final WorkspacePlanContextService workspacePlanContextService;
    private final PricingApiMapper pricingApiMapper;

    public WorkspaceSubscriptionController(
            WorkspacePlanContextService workspacePlanContextService,
            PricingApiMapper pricingApiMapper
    ) {
        this.workspacePlanContextService = workspacePlanContextService;
        this.pricingApiMapper = pricingApiMapper;
    }

    @GetMapping("/subscription")
    @Operation(summary = "Get workspace subscription and active plan context")
    public ApiResponse<WorkspacePlanContextResponse> getWorkspaceSubscription(@PathVariable UUID workspaceId) {
        return ApiResponse.success(pricingApiMapper.toWorkspacePlanContextResponse(
                workspacePlanContextService.getWorkspacePlanContext(workspaceId)));
    }
}
