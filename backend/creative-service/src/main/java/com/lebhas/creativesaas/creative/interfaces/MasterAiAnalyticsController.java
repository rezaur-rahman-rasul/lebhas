package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.ai.application.AiAnalyticsMonitoringQueryService;
import com.lebhas.ai.application.dto.AiFailureLogView;
import com.lebhas.ai.application.dto.AiLayerAnalyticsView;
import com.lebhas.ai.application.dto.DynamicRoutingOptimizationResult;
import com.lebhas.ai.application.dto.ProviderHealthSnapshot;
import com.lebhas.ai.application.dto.ProviderMetricsSnapshot;
import com.lebhas.ai.application.dto.QualityScoreResult;
import com.lebhas.ai.application.dto.WorkspaceAiUsageView;
import com.lebhas.ai.domain.AiFailureType;
import com.lebhas.creativesaas.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/master/ai")
@Tag(name = "Master AI Analytics")
@SecurityRequirement(name = "bearerAuth")
public class MasterAiAnalyticsController {

    private final AiAnalyticsMonitoringQueryService queryService;

    public MasterAiAnalyticsController(AiAnalyticsMonitoringQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/providers/metrics")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "List AI provider metrics")
    public ApiResponse<List<ProviderMetricsSnapshot>> listProviderMetrics() {
        return ApiResponse.success(queryService.listProviderMetricsForMaster());
    }

    @GetMapping("/providers/{providerId}/health")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Get AI provider health")
    public ApiResponse<ProviderHealthSnapshot> getProviderHealth(@PathVariable UUID providerId) {
        return ApiResponse.success(queryService.getProviderHealthForMaster(providerId));
    }

    @GetMapping("/layers/{layerId}/analytics")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Get AI layer analytics")
    public ApiResponse<List<AiLayerAnalyticsView>> getLayerAnalytics(@PathVariable UUID layerId) {
        return ApiResponse.success(queryService.getLayerAnalyticsForMaster(layerId));
    }

    @GetMapping("/workspaces/{workspaceId}/usage")
    @PreAuthorize("hasRole('MASTER') or hasRole('ADMIN') or hasAuthority('WORKSPACE_VIEW')")
    @Operation(summary = "Get workspace AI usage")
    public ApiResponse<WorkspaceAiUsageView> getWorkspaceUsage(@PathVariable UUID workspaceId) {
        return ApiResponse.success(queryService.getWorkspaceUsage(workspaceId));
    }

    @GetMapping("/generated-versions/{generatedVersionId}/quality-score")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Get generated version AI quality score")
    public ApiResponse<QualityScoreResult> getQualityScore(@PathVariable UUID generatedVersionId) {
        return ApiResponse.success(queryService.getQualityScoreForMaster(generatedVersionId));
    }

    @GetMapping("/failures")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "List AI failure logs")
    public ApiResponse<List<AiFailureLogView>> listFailures(
            @RequestParam(required = false) UUID providerId,
            @RequestParam(required = false) UUID layerId,
            @RequestParam(required = false) UUID creativeRequestId,
            @RequestParam(required = false) AiFailureType failureType,
            @RequestParam(required = false) Integer limit
    ) {
        return ApiResponse.success(queryService.listFailuresForMaster(
                providerId,
                layerId,
                creativeRequestId,
                failureType,
                limit));
    }

    @GetMapping("/routing/recommendations")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Get dynamic AI routing recommendations")
    public ApiResponse<DynamicRoutingOptimizationResult> getRoutingRecommendations(
            @RequestParam(required = false) UUID workspaceId,
            @RequestParam UUID layerId,
            @RequestParam(required = false) UUID creativeRequestId,
            @RequestParam(required = false) BigDecimal requestedUnits
    ) {
        return ApiResponse.success(queryService.recommendRoutingForMaster(
                workspaceId,
                layerId,
                creativeRequestId,
                requestedUnits));
    }
}
