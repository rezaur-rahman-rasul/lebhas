package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.generation.application.CreativeOutputService;
import com.lebhas.creativesaas.generation.application.dto.CreativeOutputUrlView;
import com.lebhas.creativesaas.generation.application.dto.CreativeOutputView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/creative-outputs")
@Tag(name = "Creative Outputs")
@SecurityRequirement(name = "bearerAuth")
public class CreativeOutputController {

    private final CreativeOutputService creativeOutputService;

    public CreativeOutputController(CreativeOutputService creativeOutputService) {
        this.creativeOutputService = creativeOutputService;
    }

    @GetMapping("/{outputId}")
    @PreAuthorize("hasAuthority('CREATIVE_GENERATE')")
    @Operation(summary = "Get a generated creative output")
    public ApiResponse<CreativeOutputView> get(
            @PathVariable UUID workspaceId,
            @PathVariable UUID outputId
    ) {
        return ApiResponse.success(creativeOutputService.get(workspaceId, outputId));
    }

    @GetMapping("/{outputId}/preview-url")
    @PreAuthorize("hasAuthority('CREATIVE_GENERATE')")
    @Operation(summary = "Generate a signed preview URL for a generated creative output")
    public ApiResponse<CreativeOutputUrlView> previewUrl(
            @PathVariable UUID workspaceId,
            @PathVariable UUID outputId
    ) {
        return ApiResponse.success(creativeOutputService.previewUrl(workspaceId, outputId));
    }

    @GetMapping("/{outputId}/download-url")
    @PreAuthorize("hasAuthority('CREATIVE_DOWNLOAD')")
    @Operation(summary = "Generate a signed download URL for a generated creative output")
    public ApiResponse<CreativeOutputUrlView> downloadUrl(
            @PathVariable UUID workspaceId,
            @PathVariable UUID outputId
    ) {
        return ApiResponse.success(creativeOutputService.downloadUrl(workspaceId, outputId));
    }
}
