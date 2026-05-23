package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.common.api.PagedResult;
import com.lebhas.creativesaas.generation.application.CreativeGenerationService;
import com.lebhas.creativesaas.generation.application.dto.CreativeGenerationListCriteria;
import com.lebhas.creativesaas.generation.application.dto.CreativeGenerationRequestView;
import com.lebhas.creativesaas.generation.application.dto.CreativeOutputView;
import com.lebhas.creativesaas.generation.application.dto.SubmitCreativeGenerationCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/creative-generations")
@Tag(name = "Creative Generations")
@SecurityRequirement(name = "bearerAuth")
public class CreativeGenerationController {

    private final CreativeGenerationService creativeGenerationService;

    public CreativeGenerationController(CreativeGenerationService creativeGenerationService) {
        this.creativeGenerationService = creativeGenerationService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CREATIVE_GENERATE')")
    @Operation(
            summary = "Submit an asynchronous creative generation request",
            description = "Queues a generation job and returns immediately. Status values: DRAFT, QUEUED, PROCESSING, COMPLETED, FAILED, CANCELLED. Supported creative types: STATIC_IMAGE, CAROUSEL_IMAGE, SHORT_VIDEO, PRODUCT_PROMO_VIDEO, STORY_CREATIVE, MOTION_GRAPHIC. Image formats: PNG, JPG, WEBP. Video foundation formats: MP4, MOV."
    )
    public ApiResponse<CreativeGenerationRequestView> submit(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateCreativeGenerationRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success("Creative generation request queued", creativeGenerationService.submit(new SubmitCreativeGenerationCommand(
                workspaceId,
                request.promptHistoryId(),
                request.sourcePrompt(),
                request.enhancedPrompt(),
                request.assetIds(),
                request.creativeType(),
                request.platform(),
                request.campaignObjective(),
                request.outputFormat(),
                request.language(),
                request.width(),
                request.height(),
                request.duration(),
                request.generationConfig(),
                request.useBrandContext(),
                resolveClientIp(httpServletRequest))));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CREATIVE_GENERATE')")
    @Operation(summary = "List workspace creative generation requests")
    public ApiResponse<PagedResult<CreativeGenerationRequestView>> list(
            @PathVariable UUID workspaceId,
            @Valid @ModelAttribute CreativeGenerationListRequest request
    ) {
        return ApiResponse.success(creativeGenerationService.list(new CreativeGenerationListCriteria(
                workspaceId,
                request.getUserId(),
                request.getStatus(),
                request.getCreativeType(),
                request.getPlatform(),
                request.getPage() == null ? 0 : request.getPage(),
                request.getSize() == null ? 20 : request.getSize())));
    }

    @GetMapping("/{requestId}")
    @PreAuthorize("hasAuthority('CREATIVE_GENERATE')")
    @Operation(summary = "Get a workspace creative generation request")
    public ApiResponse<CreativeGenerationRequestView> get(
            @PathVariable UUID workspaceId,
            @PathVariable UUID requestId
    ) {
        return ApiResponse.success(creativeGenerationService.get(workspaceId, requestId));
    }

    @GetMapping("/{requestId}/outputs")
    @PreAuthorize("hasAuthority('CREATIVE_GENERATE')")
    @Operation(summary = "List generated outputs for a request")
    public ApiResponse<List<CreativeOutputView>> outputs(
            @PathVariable UUID workspaceId,
            @PathVariable UUID requestId
    ) {
        return ApiResponse.success(creativeGenerationService.outputs(workspaceId, requestId));
    }

    @PostMapping("/{requestId}/retry")
    @PreAuthorize("hasAuthority('CREATIVE_GENERATE')")
    @Operation(summary = "Retry a failed or cancelled creative generation request")
    public ApiResponse<CreativeGenerationRequestView> retry(
            @PathVariable UUID workspaceId,
            @PathVariable UUID requestId
    ) {
        return ApiResponse.success("Creative generation retry queued", creativeGenerationService.retry(workspaceId, requestId));
    }

    @PostMapping("/{requestId}/cancel")
    @PreAuthorize("hasAuthority('CREATIVE_GENERATE')")
    @Operation(summary = "Cancel a queued or processing creative generation request")
    public ApiResponse<CreativeGenerationRequestView> cancel(
            @PathVariable UUID workspaceId,
            @PathVariable UUID requestId
    ) {
        return ApiResponse.success("Creative generation cancellation requested", creativeGenerationService.cancel(workspaceId, requestId));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            int separator = forwardedFor.indexOf(',');
            return (separator >= 0 ? forwardedFor.substring(0, separator) : forwardedFor).trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return StringUtils.hasText(realIp) ? realIp.trim() : request.getRemoteAddr();
    }
}
