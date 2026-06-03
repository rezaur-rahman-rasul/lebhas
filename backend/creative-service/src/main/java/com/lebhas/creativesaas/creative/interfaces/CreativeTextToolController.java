package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.common.api.PagedResult;
import com.lebhas.creativesaas.texttool.application.CreativeTextToolService;
import com.lebhas.creativesaas.texttool.application.dto.CreativeTextToolCommand;
import com.lebhas.creativesaas.texttool.application.dto.CreativeTextToolHistoryView;
import com.lebhas.creativesaas.texttool.application.dto.CreativeTextToolOutputView;
import com.lebhas.creativesaas.texttool.application.dto.CreativeTextToolRequest;
import com.lebhas.creativesaas.texttool.domain.CreativeTextToolType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Tag(name = "Creative Text Tools")
@SecurityRequirement(name = "bearerAuth")
public class CreativeTextToolController {

    private final CreativeTextToolService creativeTextToolService;

    public CreativeTextToolController(CreativeTextToolService creativeTextToolService) {
        this.creativeTextToolService = creativeTextToolService;
    }

    @PostMapping("/api/v1/workspaces/{workspaceId}/projects/{projectId}/text-tools/post")
    @PreAuthorize("hasAuthority('PROMPT_INTELLIGENCE_USE')")
    public ApiResponse<CreativeTextToolOutputView> post(
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId,
            @Valid @RequestBody CreativeTextToolApiRequest request
    ) {
        return ApiResponse.success(creativeTextToolService.generate(command(workspaceId, projectId, CreativeTextToolType.POST, request)));
    }

    @PostMapping("/api/v1/workspaces/{workspaceId}/projects/{projectId}/text-tools/caption")
    @PreAuthorize("hasAuthority('PROMPT_INTELLIGENCE_USE')")
    public ApiResponse<CreativeTextToolOutputView> caption(
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId,
            @Valid @RequestBody CreativeTextToolApiRequest request
    ) {
        return ApiResponse.success(creativeTextToolService.generate(command(workspaceId, projectId, CreativeTextToolType.CAPTION, request)));
    }

    @PostMapping("/api/v1/workspaces/{workspaceId}/projects/{projectId}/text-tools/ads-copy")
    @PreAuthorize("hasAuthority('PROMPT_INTELLIGENCE_USE')")
    public ApiResponse<CreativeTextToolOutputView> adsCopy(
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId,
            @Valid @RequestBody CreativeTextToolApiRequest request
    ) {
        return ApiResponse.success(creativeTextToolService.generate(command(workspaceId, projectId, CreativeTextToolType.ADS_COPY, request)));
    }

    @PostMapping("/api/v1/workspaces/{workspaceId}/projects/{projectId}/text-tools/hashtags")
    @PreAuthorize("hasAuthority('PROMPT_INTELLIGENCE_USE')")
    public ApiResponse<CreativeTextToolOutputView> hashtags(
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId,
            @Valid @RequestBody CreativeTextToolApiRequest request
    ) {
        return ApiResponse.success(creativeTextToolService.generate(command(workspaceId, projectId, CreativeTextToolType.HASHTAGS, request)));
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/projects/{projectId}/text-tools/history")
    @PreAuthorize("hasAuthority('PROMPT_INTELLIGENCE_USE')")
    public ApiResponse<PagedResult<CreativeTextToolHistoryView>> history(
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
        return ApiResponse.success(creativeTextToolService.history(workspaceId, projectId, pageable));
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/text-tools/{textToolOutputId}")
    @PreAuthorize("hasAuthority('PROMPT_INTELLIGENCE_USE')")
    public ApiResponse<CreativeTextToolOutputView> getOutput(
            @PathVariable UUID workspaceId,
            @PathVariable UUID textToolOutputId
    ) {
        return ApiResponse.success(creativeTextToolService.getOutput(workspaceId, textToolOutputId));
    }

    private CreativeTextToolCommand command(UUID workspaceId, UUID projectId, CreativeTextToolType type, CreativeTextToolApiRequest request) {
        return new CreativeTextToolCommand(
                workspaceId,
                projectId,
                type,
                new CreativeTextToolRequest(
                        request.brandId(),
                        request.productServiceId(),
                        request.platform(),
                        request.language(),
                        request.tone(),
                        request.campaignObjective(),
                        request.sourceIdea(),
                        request.qualityMode(),
                        request.selectedAssetIds()));
    }
}
