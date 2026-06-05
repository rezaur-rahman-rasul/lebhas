package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.common.api.PagedResult;
import com.lebhas.creativesaas.imagecreative.application.ProductImageCreativeService;
import com.lebhas.creativesaas.generation.application.CreativePipelineRunQueryService;
import com.lebhas.creativesaas.generation.application.dto.CreativePipelineRunView;
import com.lebhas.creativesaas.imagecreative.application.dto.ImageCreativeCostPreviewView;
import com.lebhas.creativesaas.imagecreative.application.dto.ImageCreativeGenerationView;
import com.lebhas.creativesaas.imagecreative.application.dto.ProductImageCreativeCommand;
import com.lebhas.creativesaas.imagecreative.application.dto.ProductImageCreativeGenerationResult;
import com.lebhas.creativesaas.imagecreative.application.dto.ProductImageCreativeRequest;
import com.lebhas.creativesaas.imagecreative.application.dto.ProductImageCreativeReadinessView;
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
@Tag(name = "Product Image Creatives")
@SecurityRequirement(name = "bearerAuth")
public class ProductImageCreativeController {

    private final ProductImageCreativeService productImageCreativeService;
    private final CreativePipelineRunQueryService pipelineRunQueryService;

    public ProductImageCreativeController(
            ProductImageCreativeService productImageCreativeService,
            CreativePipelineRunQueryService pipelineRunQueryService
    ) {
        this.productImageCreativeService = productImageCreativeService;
        this.pipelineRunQueryService = pipelineRunQueryService;
    }

    @PostMapping("/api/v1/workspaces/{workspaceId}/projects/{projectId}/image-creatives/generate")
    @PreAuthorize("hasAuthority('CREATIVE_GENERATE')")
    public ApiResponse<ProductImageCreativeGenerationResult> generate(
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId,
            @Valid @RequestBody ProductImageCreativeApiRequest request
    ) {
        return ApiResponse.success(productImageCreativeService.generate(command(workspaceId, projectId, request)));
    }

    @PostMapping("/api/v1/workspaces/{workspaceId}/projects/{projectId}/image-creatives/preview-cost")
    @PreAuthorize("hasAuthority('CREATIVE_GENERATE')")
    public ApiResponse<ImageCreativeCostPreviewView> previewCost(
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId,
            @Valid @RequestBody ProductImageCreativeApiRequest request
    ) {
        return ApiResponse.success(productImageCreativeService.previewCost(command(workspaceId, projectId, request)));
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/projects/{projectId}/image-creatives/readiness")
    @PreAuthorize("hasAuthority('CREATIVE_GENERATE')")
    public ApiResponse<ProductImageCreativeReadinessView> readiness(
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId,
            @RequestParam(required = false) UUID productAssetId,
            @RequestParam(defaultValue = "BASIC") String qualityMode,
            @RequestParam(defaultValue = "1") int requestedVersionCount
    ) {
        return ApiResponse.success(productImageCreativeService.readiness(
                workspaceId,
                projectId,
                productAssetId,
                qualityMode,
                Math.max(1, requestedVersionCount)));
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/projects/{projectId}/image-creatives/history")
    @PreAuthorize("hasAuthority('WORKSPACE_VIEW')")
    public ApiResponse<PagedResult<ImageCreativeGenerationView>> history(
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
        return ApiResponse.success(productImageCreativeService.history(workspaceId, projectId, pageable));
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/creative-generator/requests/{requestId}/pipeline")
    @PreAuthorize("hasAuthority('WORKSPACE_VIEW')")
    public ApiResponse<CreativePipelineRunView> pipeline(
            @PathVariable UUID workspaceId,
            @PathVariable UUID requestId
    ) {
        return ApiResponse.success(pipelineRunQueryService.getLatestByCreativeRequest(workspaceId, requestId));
    }

    private ProductImageCreativeCommand command(UUID workspaceId, UUID projectId, ProductImageCreativeApiRequest request) {
        return new ProductImageCreativeCommand(
                workspaceId,
                projectId,
                new ProductImageCreativeRequest(
                        request.promptDraftId(),
                        request.sourcePrompt(),
                        request.productAssetId(),
                        request.creativeFormat(),
                        request.platform(),
                        request.language(),
                        request.qualityMode(),
                        request.requestedVersionCount(),
                        request.stylePreset(),
                        request.backgroundStyle(),
                        request.cta()));
    }
}
