package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.ai.application.AiAnalyticsMonitoringQueryService;
import com.lebhas.ai.application.dto.AiFailureLogView;
import com.lebhas.ai.application.dto.AiCostUsageSummary;
import com.lebhas.ai.application.dto.AiFailuresSummary;
import com.lebhas.ai.application.dto.AiLayerAnalyticsView;
import com.lebhas.ai.application.dto.DynamicRoutingOptimizationResult;
import com.lebhas.ai.application.dto.LayerAnalyticsSummary;
import com.lebhas.ai.application.dto.MasterMonitoringResponse;
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
        return ApiResponse.success("Layer analytics loaded", queryService.getLayerAnalyticsForMaster(layerId));
    }

    @GetMapping("/layer-analytics")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "List AI layer analytics")
    public ApiResponse<MasterMonitoringResponse<LayerAnalyticsSummary, AiLayerAnalyticsView>> listLayerAnalytics() {
        List<AiLayerAnalyticsView> items = queryService.listLayerAnalyticsForMaster();
        BigDecimal totalCost = items.stream()
                .map(item -> item.avgExecutionCostUsd() == null ? BigDecimal.ZERO : item.avgExecutionCostUsd())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDuration = items.stream()
                .map(item -> item.avgExecutionTimeMs() == null ? BigDecimal.ZERO : item.avgExecutionTimeMs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LayerAnalyticsSummary summary = new LayerAnalyticsSummary(
                items.stream().mapToLong(AiLayerAnalyticsView::totalExecutions).sum(),
                average(totalCost, items.size()),
                average(totalDuration, items.size()),
                items.stream().mapToLong(AiLayerAnalyticsView::failedExecutions).sum());
        return ApiResponse.success(items.isEmpty() ? "No records found" : "Layer analytics loaded",
                MasterMonitoringResponse.of(summary, items));
    }

    @GetMapping("/cost-usage")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "List AI cost usage")
    public ApiResponse<MasterMonitoringResponse<AiCostUsageSummary, WorkspaceAiUsageView>> listCostUsage() {
        List<WorkspaceAiUsageView> items = queryService.listWorkspaceUsageForMaster();
        BigDecimal totalCost = items.stream()
                .map(item -> item.totalEstimatedCostUsd() == null ? BigDecimal.ZERO : item.totalEstimatedCostUsd())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalRuns = items.stream().mapToLong(WorkspaceAiUsageView::totalGenerationRequests).sum();
        AiCostUsageSummary summary = new AiCostUsageSummary(
                totalCost,
                totalRuns,
                totalRuns == 0 ? null : totalCost.divide(BigDecimal.valueOf(totalRuns), java.math.RoundingMode.HALF_UP));
        return ApiResponse.success(items.isEmpty() ? "No records found" : "AI cost usage loaded",
                MasterMonitoringResponse.of(summary, items));
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
    public ApiResponse<MasterMonitoringResponse<AiFailuresSummary, AiFailureLogView>> listFailures(
            @RequestParam(required = false) UUID providerId,
            @RequestParam(required = false) UUID layerId,
            @RequestParam(required = false) UUID creativeRequestId,
            @RequestParam(required = false) AiFailureType failureType,
            @RequestParam(required = false) Integer limit
    ) {
        List<AiFailureLogView> items = queryService.listFailuresForMaster(
                providerId,
                layerId,
                creativeRequestId,
                failureType,
                limit);
        AiFailuresSummary summary = new AiFailuresSummary(
                items.size(),
                items.stream().mapToLong(AiFailureLogView::retryAttempt).sum(),
                items.stream().filter(AiFailureLogView::fallbackTriggered).count());
        return ApiResponse.success(items.isEmpty() ? "No records found" : "AI failures loaded",
                MasterMonitoringResponse.of(summary, items));
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

    private BigDecimal average(BigDecimal total, int itemCount) {
        if (itemCount <= 0) {
            return null;
        }
        return total.divide(BigDecimal.valueOf(itemCount), java.math.RoundingMode.HALF_UP);
    }
}
