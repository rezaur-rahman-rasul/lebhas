package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.ai.application.MasterCreativePipelineManagementService;
import com.lebhas.ai.application.dto.CreativePipelineCommand;
import com.lebhas.ai.application.dto.CreativePipelineLayerCommand;
import com.lebhas.ai.application.dto.CreativePipelineView;
import com.lebhas.ai.application.dto.LayerCostPolicyCommand;
import com.lebhas.ai.application.dto.LayerQualityPolicyCommand;
import com.lebhas.ai.application.dto.LayerRoutingPolicyCommand;
import com.lebhas.ai.application.dto.LayerToolMappingCommand;
import com.lebhas.creativesaas.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/master/creative-pipelines")
@Tag(name = "Master Creative Pipelines")
@SecurityRequirement(name = "bearerAuth")
public class MasterCreativePipelineController {

    private final MasterCreativePipelineManagementService pipelineService;

    public MasterCreativePipelineController(MasterCreativePipelineManagementService pipelineService) {
        this.pipelineService = pipelineService;
    }

    @PostMapping
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Create a creative pipeline")
    public ApiResponse<CreativePipelineView> createPipeline(@Valid @RequestBody CreateCreativePipelineRequest request) {
        return ApiResponse.success(pipelineService.createPipeline(new CreativePipelineCommand(
                request.pipelineCode(),
                request.pipelineName(),
                request.description(),
                request.status(),
                request.active(),
                request.version(),
                request.metadata())));
    }

    @GetMapping
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "List creative pipelines")
    public ApiResponse<List<CreativePipelineView>> listPipelines() {
        return ApiResponse.success(pipelineService.listPipelines());
    }

    @GetMapping("/{pipelineId}")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Get creative pipeline")
    public ApiResponse<CreativePipelineView> getPipeline(@PathVariable UUID pipelineId) {
        return ApiResponse.success(pipelineService.getPipeline(pipelineId));
    }

    @PutMapping("/{pipelineId}")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Update a creative pipeline")
    public ApiResponse<CreativePipelineView> updatePipeline(
            @PathVariable UUID pipelineId,
            @Valid @RequestBody UpdateCreativePipelineRequest request
    ) {
        return ApiResponse.success(pipelineService.updatePipeline(pipelineId, new CreativePipelineCommand(
                null,
                request.pipelineName(),
                request.description(),
                request.status(),
                request.active(),
                request.version(),
                request.metadata())));
    }

    @DeleteMapping("/{pipelineId}")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Disable a creative pipeline")
    public ApiResponse<CreativePipelineView> disablePipeline(@PathVariable UUID pipelineId) {
        return ApiResponse.success(pipelineService.disablePipeline(pipelineId));
    }

    @PostMapping("/{pipelineId}/layers")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Create a creative pipeline layer")
    public ApiResponse<CreativePipelineView> createLayer(
            @PathVariable UUID pipelineId,
            @Valid @RequestBody UpsertCreativePipelineLayerRequest request
    ) {
        return ApiResponse.success(pipelineService.createLayer(pipelineId, toLayerCommand(request)));
    }

    @PutMapping("/{pipelineId}/layers/{layerId}")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Update a creative pipeline layer")
    public ApiResponse<CreativePipelineView> updateLayer(
            @PathVariable UUID pipelineId,
            @PathVariable UUID layerId,
            @Valid @RequestBody UpsertCreativePipelineLayerRequest request
    ) {
        return ApiResponse.success(pipelineService.updateLayer(pipelineId, layerId, toLayerCommand(request)));
    }

    @DeleteMapping("/{pipelineId}/layers/{layerId}")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Disable a creative pipeline layer")
    public ApiResponse<CreativePipelineView> disableLayer(
            @PathVariable UUID pipelineId,
            @PathVariable UUID layerId
    ) {
        return ApiResponse.success(pipelineService.disableLayer(pipelineId, layerId));
    }

    @PutMapping("/{pipelineId}/layers/{layerId}/tool-mappings")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Assign provider tool mapping to a pipeline layer")
    public ApiResponse<CreativePipelineView> assignTool(
            @PathVariable UUID pipelineId,
            @PathVariable UUID layerId,
            @Valid @RequestBody AssignLayerToolRequest request
    ) {
        return ApiResponse.success(pipelineService.assignTool(pipelineId, layerId, new LayerToolMappingCommand(
                request.providerId(),
                request.modelId(),
                request.capabilityId(),
                request.mappingCode(),
                request.priorityOrder(),
                request.routingWeight(),
                request.enabled(),
                request.fallbackEligible(),
                request.routingMetadata())));
    }

    @PutMapping("/{pipelineId}/layers/{layerId}/routing-policy")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Configure layer routing policy")
    public ApiResponse<CreativePipelineView> configureRoutingPolicy(
            @PathVariable UUID pipelineId,
            @PathVariable UUID layerId,
            @Valid @RequestBody ConfigureLayerRoutingPolicyRequest request
    ) {
        return ApiResponse.success(pipelineService.configureRoutingPolicy(pipelineId, layerId, toRoutingCommand(request)));
    }

    @PutMapping("/{pipelineId}/layers/{layerId}/fallback-policy")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Configure layer fallback policy")
    public ApiResponse<CreativePipelineView> configureFallbackPolicy(
            @PathVariable UUID pipelineId,
            @PathVariable UUID layerId,
            @Valid @RequestBody ConfigureLayerRoutingPolicyRequest request
    ) {
        return ApiResponse.success(pipelineService.configureFallbackPolicy(pipelineId, layerId, toRoutingCommand(request)));
    }

    @PutMapping("/{pipelineId}/layers/{layerId}/cost-policy")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Configure layer cost policy")
    public ApiResponse<CreativePipelineView> configureCostPolicy(
            @PathVariable UUID pipelineId,
            @PathVariable UUID layerId,
            @Valid @RequestBody ConfigureLayerCostPolicyRequest request
    ) {
        return ApiResponse.success(pipelineService.configureCostPolicy(pipelineId, layerId, new LayerCostPolicyCommand(
                request.policyCode(),
                request.enabled(),
                request.priorityOrder(),
                request.currency(),
                request.maxCostPerRun(),
                request.costRules(),
                request.budgetMetadata())));
    }

    @PutMapping("/{pipelineId}/layers/{layerId}/quality-policy")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Configure layer quality policy")
    public ApiResponse<CreativePipelineView> configureQualityPolicy(
            @PathVariable UUID pipelineId,
            @PathVariable UUID layerId,
            @Valid @RequestBody ConfigureLayerQualityPolicyRequest request
    ) {
        return ApiResponse.success(pipelineService.configureQualityPolicy(pipelineId, layerId, new LayerQualityPolicyCommand(
                request.policyCode(),
                request.enabled(),
                request.priorityOrder(),
                request.minQualityScore(),
                request.qualityRules(),
                request.evaluationMetadata())));
    }

    @PutMapping("/{pipelineId}/layers/{layerId}/retry-policy")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Configure layer retry policy")
    public ApiResponse<CreativePipelineView> configureRetryPolicy(
            @PathVariable UUID pipelineId,
            @PathVariable UUID layerId,
            @Valid @RequestBody ConfigureLayerRetryPolicyRequest request
    ) {
        return ApiResponse.success(pipelineService.configureRetryPolicy(
                pipelineId,
                layerId,
                request.retryable(),
                request.configuration()));
    }

    private CreativePipelineLayerCommand toLayerCommand(UpsertCreativePipelineLayerRequest request) {
        return new CreativePipelineLayerCommand(
                request.layerType(),
                request.layerCode(),
                request.layerName(),
                request.sortOrder(),
                request.enabled(),
                request.required(),
                request.retryable(),
                request.configuration());
    }

    private LayerRoutingPolicyCommand toRoutingCommand(ConfigureLayerRoutingPolicyRequest request) {
        return new LayerRoutingPolicyCommand(
                request.policyCode(),
                request.routingStrategy(),
                request.priorityOrder(),
                request.enabled(),
                request.conditions(),
                request.rules());
    }
}
