package com.lebhas.creativesaas.billing.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.common.api.PagedResult;
import com.lebhas.creativesaas.usage.application.MasterUsageQueryService;
import com.lebhas.creativesaas.usage.application.dto.PlanUtilizationReportView;
import com.lebhas.creativesaas.usage.application.dto.UsageBillingLogView;
import com.lebhas.creativesaas.usage.application.dto.WorkspaceUsageSummaryView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/master/usage")
@Tag(name = "Master Usage")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('MASTER')")
public class MasterUsageController {

    private final MasterUsageQueryService masterUsageQueryService;

    public MasterUsageController(MasterUsageQueryService masterUsageQueryService) {
        this.masterUsageQueryService = masterUsageQueryService;
    }

    @GetMapping("/overview")
    @Operation(summary = "Load Master usage overview")
    public ApiResponse<Map<String, Object>> getOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month
    ) {
        Pageable firstPage = pageable(0, 20);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("hasData", false);
        data.put("message", "No master usage summary has been reported yet.");
        data.put("workspaces", masterUsageQueryService.workspaceUsage(month, firstPage));
        data.put("aiCosts", masterUsageQueryService.aiCosts(firstPage));
        data.put("topCostWorkspaces", masterUsageQueryService.topCostWorkspaces(month, firstPage));
        data.put("planUtilization", masterUsageQueryService.planUtilization(month));
        return ApiResponse.success("Master usage overview loaded", data);
    }

    @GetMapping("/workspaces")
    @Operation(summary = "List workspace usage summaries")
    public ApiResponse<PagedResult<WorkspaceUsageSummaryView>> getWorkspaceUsage(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(masterUsageQueryService.workspaceUsage(month, pageable(page, size)));
    }

    @GetMapping("/workspaces/{workspaceId}")
    @Operation(summary = "Get one workspace usage summary")
    public ApiResponse<WorkspaceUsageSummaryView> getWorkspaceUsage(
            @PathVariable UUID workspaceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month
    ) {
        return ApiResponse.success(masterUsageQueryService.workspaceUsage(workspaceId, month));
    }

    @GetMapping("/ai-costs")
    @Operation(summary = "List recorded AI cost billing logs")
    public ApiResponse<PagedResult<UsageBillingLogView>> getAiCosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(masterUsageQueryService.aiCosts(pageable(page, size)));
    }

    @GetMapping("/top-cost-workspaces")
    @Operation(summary = "List top AI cost workspaces")
    public ApiResponse<PagedResult<WorkspaceUsageSummaryView>> getTopCostWorkspaces(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(masterUsageQueryService.topCostWorkspaces(month, pageable(page, size)));
    }

    @GetMapping("/plan-utilization")
    @Operation(summary = "List active workspace plan utilization")
    public ApiResponse<List<PlanUtilizationReportView>> getPlanUtilization(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month
    ) {
        return ApiResponse.success(masterUsageQueryService.planUtilization(month));
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 100)));
    }
}
