package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.ai.application.MasterCreativePipelineManagementService;
import com.lebhas.ai.application.dto.CreativePipelineLayerCommand;
import com.lebhas.ai.application.dto.CreativePipelineLayerView;
import com.lebhas.ai.application.dto.CreativePipelineView;
import com.lebhas.ai.domain.CreativeLayerType;
import com.lebhas.creativesaas.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/master/creative-layers")
@Tag(name = "Master Creative Layers")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('MASTER')")
public class MasterCreativeLayerController {

    private final MasterCreativePipelineManagementService pipelineService;

    public MasterCreativeLayerController(MasterCreativePipelineManagementService pipelineService) {
        this.pipelineService = pipelineService;
    }

    @PostMapping
    @Operation(summary = "Create a creative layer")
    public ApiResponse<CreativePipelineView> createLayer(@Valid @RequestBody MasterCreativeLayerRequest request) {
        return ApiResponse.success(pipelineService.createLayer(request.pipelineId(), request.toCommand()));
    }

    @GetMapping
    @Operation(summary = "List creative layers")
    public ApiResponse<List<CreativePipelineLayerView>> listLayers() {
        return ApiResponse.success("Creative layers loaded", pipelineService.listPipelines().stream()
                .flatMap(pipeline -> pipeline.layers().stream())
                .toList());
    }

    @GetMapping("/{layerId}")
    @Operation(summary = "Get creative layer")
    public ApiResponse<CreativePipelineLayerView> getLayer(@PathVariable UUID layerId) {
        return pipelineService.listPipelines().stream()
                .flatMap(pipeline -> pipeline.layers().stream())
                .filter(layer -> layer.id().equals(layerId))
                .findFirst()
                .map(layer -> ApiResponse.success("Creative layer loaded", layer))
                .orElseGet(() -> ApiResponse.success("Creative layer not found", null));
    }

    @PutMapping("/{layerId}")
    @Operation(summary = "Update a creative layer")
    public ApiResponse<CreativePipelineView> updateLayer(
            @PathVariable UUID layerId,
            @Valid @RequestBody MasterCreativeLayerRequest request
    ) {
        return ApiResponse.success(pipelineService.updateLayer(request.pipelineId(), layerId, request.toCommand()));
    }

    @PatchMapping("/{layerId}/enable")
    @Operation(summary = "Enable a creative layer")
    public ApiResponse<Map<String, Object>> enableLayer(@PathVariable UUID layerId) {
        return foundationLayerMutation(layerId, "enable");
    }

    @PatchMapping("/{layerId}/disable")
    @Operation(summary = "Disable a creative layer")
    public ApiResponse<Map<String, Object>> disableLayer(@PathVariable UUID layerId) {
        return foundationLayerMutation(layerId, "disable");
    }

    @PatchMapping("/{layerId}/status")
    @Operation(summary = "Update creative layer status")
    public ApiResponse<Map<String, Object>> updateLayerStatus(@PathVariable UUID layerId) {
        return foundationLayerMutation(layerId, "status");
    }

    @PostMapping("/reorder")
    @Operation(summary = "Reorder creative layers")
    public ApiResponse<Map<String, Object>> reorderLayers(@RequestBody(required = false) Map<String, Object> request) {
        return ApiResponse.success("Creative layer reorder foundation endpoint is available",
                Map.of("status", "foundation_pending"));
    }

    private ApiResponse<Map<String, Object>> foundationLayerMutation(UUID layerId, String action) {
        return ApiResponse.success("Creative layer " + action + " foundation endpoint is available",
                Map.of("layerId", layerId, "status", "foundation_pending"));
    }

    public record MasterCreativeLayerRequest(
            @NotNull UUID pipelineId,
            @NotNull CreativeLayerType layerType,
            @NotBlank @Size(max = 120) String layerCode,
            @NotBlank @Size(max = 180) String layerName,
            @Min(1) int sortOrder,
            boolean enabled,
            boolean required,
            boolean retryable,
            @NotNull Map<String, Object> configuration
    ) {
        CreativePipelineLayerCommand toCommand() {
            return new CreativePipelineLayerCommand(
                    layerType,
                    layerCode,
                    layerName,
                    sortOrder,
                    enabled,
                    required,
                    retryable,
                    configuration);
        }
    }
}
