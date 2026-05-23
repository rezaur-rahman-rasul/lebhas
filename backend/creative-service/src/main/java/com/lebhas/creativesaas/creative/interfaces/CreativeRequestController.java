package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.creativerequest.application.CreativeRequestService;
import com.lebhas.creativesaas.creativerequest.application.dto.CancelCreativeRequestCommand;
import com.lebhas.creativesaas.creativerequest.application.dto.CreateCreativeRequestCommand;
import com.lebhas.creativesaas.creativerequest.application.dto.CreativeRequestResponse;
import com.lebhas.creativesaas.creativerequest.application.dto.RetryCreativeRequestCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}")
@Tag(name = "Creative Requests")
@SecurityRequirement(name = "bearerAuth")
public class CreativeRequestController {

    private final CreativeRequestService creativeRequestService;
    private final Day5ApiMapper day5ApiMapper;

    public CreativeRequestController(
            CreativeRequestService creativeRequestService,
            Day5ApiMapper day5ApiMapper
    ) {
        this.creativeRequestService = creativeRequestService;
        this.day5ApiMapper = day5ApiMapper;
    }

    @PostMapping("/projects/{projectId}/creative-requests")
    @PreAuthorize("hasAuthority('CREATIVE_REQUEST_CREATE')")
    @Operation(
            summary = "Create a creative request",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Creative request queued"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(hidden = true))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(hidden = true)))
            })
    public ApiResponse<CreativeRequestResourceResponse> createCreativeRequest(
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateCreativeRequestRequest request
    ) {
        CreativeRequestResponse response = creativeRequestService.createCreativeRequest(new CreateCreativeRequestCommand(
                workspaceId,
                null,
                null,
                projectId,
                request.requestName(),
                request.sourcePrompt(),
                request.enhancedPrompt(),
                request.languagePreference(),
                request.creativeObjective(),
                request.targetPlatform(),
                request.requestedFormat(),
                request.requestedVersions(),
                request.selectedAssetIds()));
        return ApiResponse.success("Creative request queued", day5ApiMapper.toCreativeRequestResponse(response));
    }

    @GetMapping("/projects/{projectId}/creative-requests")
    @PreAuthorize("hasAuthority('WORKSPACE_VIEW')")
    @Operation(
            summary = "List creative requests for a project",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Creative requests returned",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CreativeRequestResourceResponse.class)))))
    public ApiResponse<List<CreativeRequestResourceResponse>> listCreativeRequests(
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId
    ) {
        return ApiResponse.success(creativeRequestService.listProjectCreativeRequests(workspaceId, projectId).stream()
                .map(day5ApiMapper::toCreativeRequestResponse)
                .toList());
    }

    @GetMapping("/creative-requests/{creativeRequestId}")
    @PreAuthorize("hasAuthority('WORKSPACE_VIEW')")
    @Operation(summary = "Get a creative request by id")
    public ApiResponse<CreativeRequestResourceResponse> getCreativeRequest(
            @PathVariable UUID workspaceId,
            @PathVariable UUID creativeRequestId
    ) {
        return ApiResponse.success(day5ApiMapper.toCreativeRequestResponse(
                creativeRequestService.getRequest(workspaceId, creativeRequestId)));
    }

    @PostMapping("/creative-requests/{creativeRequestId}/cancel")
    @PreAuthorize("hasAuthority('CREATIVE_REQUEST_CREATE')")
    @Operation(summary = "Cancel a queued creative request")
    public ApiResponse<CreativeRequestResourceResponse> cancelCreativeRequest(
            @PathVariable UUID workspaceId,
            @PathVariable UUID creativeRequestId
    ) {
        return ApiResponse.success(
                "Creative request cancelled",
                day5ApiMapper.toCreativeRequestResponse(
                        creativeRequestService.cancelQueuedRequest(new CancelCreativeRequestCommand(workspaceId, creativeRequestId))));
    }

    @PostMapping("/creative-requests/{creativeRequestId}/retry")
    @PreAuthorize("hasAuthority('CREATIVE_REQUEST_CREATE')")
    @Operation(summary = "Retry a failed creative request")
    public ApiResponse<CreativeRequestResourceResponse> retryCreativeRequest(
            @PathVariable UUID workspaceId,
            @PathVariable UUID creativeRequestId
    ) {
        return ApiResponse.success(
                "Creative request retry queued",
                day5ApiMapper.toCreativeRequestResponse(
                        creativeRequestService.retryFailedRequest(new RetryCreativeRequestCommand(workspaceId, creativeRequestId))));
    }
}
