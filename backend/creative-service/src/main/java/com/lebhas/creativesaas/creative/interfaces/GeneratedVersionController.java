package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.generatedversion.application.GeneratedVersionQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
@RequestMapping("/api/v1/workspaces/{workspaceId}")
@Tag(name = "Generated Versions")
@SecurityRequirement(name = "bearerAuth")
public class GeneratedVersionController {

    private final GeneratedVersionQueryService generatedVersionQueryService;
    private final Day5ApiMapper day5ApiMapper;

    public GeneratedVersionController(
            GeneratedVersionQueryService generatedVersionQueryService,
            Day5ApiMapper day5ApiMapper
    ) {
        this.generatedVersionQueryService = generatedVersionQueryService;
        this.day5ApiMapper = day5ApiMapper;
    }

    @GetMapping("/creative-requests/{creativeRequestId}/generated-versions")
    @PreAuthorize("hasAuthority('WORKSPACE_VIEW')")
    @Operation(
            summary = "List generated versions for a creative request",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Generated versions returned",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = GeneratedVersionResponse.class)))))
    public ApiResponse<List<GeneratedVersionResponse>> listGeneratedVersions(
            @PathVariable UUID workspaceId,
            @PathVariable UUID creativeRequestId
    ) {
        return ApiResponse.success(day5ApiMapper.toGeneratedVersionResponses(
                generatedVersionQueryService.listByCreativeRequest(workspaceId, creativeRequestId)));
    }

    @GetMapping("/generated-versions/{generatedVersionId}")
    @PreAuthorize("hasAuthority('WORKSPACE_VIEW')")
    @Operation(summary = "Get a generated version by id")
    public ApiResponse<GeneratedVersionResponse> getGeneratedVersion(
            @PathVariable UUID workspaceId,
            @PathVariable UUID generatedVersionId
    ) {
        return ApiResponse.success(day5ApiMapper.toGeneratedVersionResponse(
                generatedVersionQueryService.getById(workspaceId, generatedVersionId)));
    }

    @GetMapping("/generated-versions/{generatedVersionId}/preview-url")
    @PreAuthorize("hasAuthority('CREATIVE_GENERATE')")
    @Operation(summary = "Generate a signed preview URL for a generated version")
    public ApiResponse<GeneratedVersionPreviewUrlResponse> previewUrl(
            @PathVariable UUID workspaceId,
            @PathVariable UUID generatedVersionId
    ) {
        return ApiResponse.success(day5ApiMapper.toGeneratedVersionPreviewUrlResponse(
                generatedVersionId,
                generatedVersionQueryService.previewUrl(workspaceId, generatedVersionId)));
    }
}
