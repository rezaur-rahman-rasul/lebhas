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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/master/dashboard")
@Tag(name = "Master Dashboard")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('MASTER')")
public class MasterDashboardController {

    private final MasterUsageQueryService masterUsageQueryService;

    public MasterDashboardController(MasterUsageQueryService masterUsageQueryService) {
        this.masterUsageQueryService = masterUsageQueryService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Load Master dashboard summary")
    public ApiResponse<Map<String, Object>> summary() {
        LocalDate month = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
        PagedResult<WorkspaceUsageSummaryView> workspaces = masterUsageQueryService.workspaceUsage(month, PageRequest.of(0, 5));
        PagedResult<UsageBillingLogView> aiCosts = masterUsageQueryService.aiCosts(PageRequest.of(0, 5));
        List<PlanUtilizationReportView> plans = masterUsageQueryService.planUtilization(month);

        Map<String, Object> providerHealth = new LinkedHashMap<>();
        providerHealth.put("totalProviders", null);
        providerHealth.put("healthy", null);
        providerHealth.put("degraded", null);
        providerHealth.put("failed", null);
        providerHealth.put("hasData", false);

        Map<String, Object> workspaceOverview = new LinkedHashMap<>();
        workspaceOverview.put("hasData", workspaces.totalItems() > 0);
        workspaceOverview.put("message", workspaces.totalItems() > 0 ? "Workspace usage loaded." : "Workspace usage has not been reported yet.");
        workspaceOverview.put("items", workspaces.items());

        Map<String, Object> creditUsage = new LinkedHashMap<>();
        creditUsage.put("hasData", !plans.isEmpty());
        creditUsage.put("message", plans.isEmpty() ? "No credit usage has been recorded yet." : "Credit usage loaded.");
        creditUsage.put("items", plans);

        Map<String, Object> readiness = new LinkedHashMap<>();
        readiness.put("ready", 0);
        readiness.put("needsAttention", 0);
        readiness.put("blocked", 0);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("accessibleWorkspaces", workspaces.totalItems());
        data.put("adminUsers", 0);
        data.put("activePlans", plans.size());
        data.put("aiGenerations", plans.stream().mapToLong(PlanUtilizationReportView::generatedVersions).sum());
        data.put("creditRevenueUsage", plans.stream().map(PlanUtilizationReportView::usedCredits).reduce(BigDecimal.ZERO, BigDecimal::add));
        data.put("systemAlerts", 0);
        data.put("workspaceOverview", workspaceOverview);
        data.put("providerHealth", providerHealth);
        data.put("creditUsage", creditUsage);
        data.put("recentSystemActivity", aiCosts.items());
        data.put("pendingIssues", List.of());
        data.put("goLiveReadiness", readiness);
        return ApiResponse.success("Master dashboard summary loaded", data);
    }
}
