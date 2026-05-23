package com.lebhas.creativesaas.workspace.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.workspace.application.WorkspaceBrandProfileService;
import com.lebhas.creativesaas.workspace.application.dto.BrandProfileView;
import com.lebhas.creativesaas.workspace.application.dto.UpdateBrandProfileCommand;
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
@RequestMapping("/api/v1/workspaces/{id}/brand-profile")
@Tag(name = "Brand Profiles")
@SecurityRequirement(name = "bearerAuth")
public class WorkspaceBrandProfileController {

    private final WorkspaceBrandProfileService workspaceBrandProfileService;

    public WorkspaceBrandProfileController(WorkspaceBrandProfileService workspaceBrandProfileService) {
        this.workspaceBrandProfileService = workspaceBrandProfileService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('WORKSPACE_VIEW')")
    @Operation(summary = "Get a workspace brand profile")
    public ApiResponse<BrandProfileView> getBrandProfile(@PathVariable UUID id) {
        return ApiResponse.success(workspaceBrandProfileService.getBrandProfile(id));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('BRAND_PROFILE_UPDATE')")
    @Operation(summary = "Update a workspace brand profile")
    public ApiResponse<BrandProfileView> updateBrandProfile(@PathVariable UUID id, @Valid @RequestBody UpdateBrandProfileRequest request) {
        return ApiResponse.success(workspaceBrandProfileService.updateBrandProfile(new UpdateBrandProfileCommand(
                id,
                request.brandName(),
                request.businessType(),
                request.industry(),
                request.targetAudience(),
                request.brandVoice(),
                request.preferredCta(),
                request.primaryColor(),
                request.secondaryColor(),
                request.website(),
                request.facebookUrl(),
                request.instagramUrl(),
                request.linkedinUrl(),
                request.tiktokUrl(),
                request.description())));
    }
}
