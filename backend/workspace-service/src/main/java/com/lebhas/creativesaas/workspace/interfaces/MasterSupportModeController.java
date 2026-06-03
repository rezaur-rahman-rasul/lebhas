package com.lebhas.creativesaas.workspace.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.identity.application.MasterSupportModeService;
import com.lebhas.creativesaas.workspace.application.dto.SupportModeView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/master/workspaces")
@Tag(name = "Master Support")
@SecurityRequirement(name = "bearerAuth")
public class MasterSupportModeController {

    private final MasterSupportModeService masterSupportModeService;

    public MasterSupportModeController(MasterSupportModeService masterSupportModeService) {
        this.masterSupportModeService = masterSupportModeService;
    }

    @PostMapping("/{workspaceId}/enter-support-mode")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Enter support mode for a workspace")
    public ApiResponse<SupportModeView> enterSupportMode(@PathVariable UUID workspaceId) {
        return ApiResponse.success(masterSupportModeService.enterSupportMode(workspaceId));
    }

    @PostMapping("/{workspaceId}/exit-support-mode")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Exit support mode for a workspace")
    public ApiResponse<SupportModeView> exitWorkspaceSupportMode(@PathVariable UUID workspaceId) {
        return ApiResponse.success(masterSupportModeService.exitSupportMode());
    }
}
