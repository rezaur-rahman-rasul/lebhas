package com.lebhas.creativesaas.billing.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.common.api.PagedResult;
import com.lebhas.creativesaas.usage.application.UsageBillingApiQueryService;
import com.lebhas.creativesaas.usage.application.dto.CreditLedgerView;
import com.lebhas.creativesaas.usage.application.dto.DownloadUsageView;
import com.lebhas.creativesaas.usage.application.dto.MonthlyUsageSnapshotView;
import com.lebhas.creativesaas.usage.application.dto.PlanUtilizationReportView;
import com.lebhas.creativesaas.usage.application.dto.ShareUsageView;
import com.lebhas.creativesaas.usage.application.dto.UsageBillingLogView;
import com.lebhas.creativesaas.usage.application.dto.WorkspaceUsageSummaryView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}")
@Tag(name = "Usage Billing")
@SecurityRequirement(name = "bearerAuth")
public class UsageBillingController {

    private final UsageBillingApiQueryService usageBillingApiQueryService;

    public UsageBillingController(UsageBillingApiQueryService usageBillingApiQueryService) {
        this.usageBillingApiQueryService = usageBillingApiQueryService;
    }

    @GetMapping("/usage/current")
    @Operation(summary = "Get current month workspace usage")
    public ApiResponse<WorkspaceUsageSummaryView> getCurrentUsage(@PathVariable UUID workspaceId) {
        return ApiResponse.success(usageBillingApiQueryService.currentUsage(workspaceId));
    }

    @GetMapping("/usage-summary/current-month")
    @Operation(summary = "Get current month workspace usage summary")
    public ApiResponse<WorkspaceUsageSummaryView> getCurrentMonthUsageSummary(@PathVariable UUID workspaceId) {
        return ApiResponse.success(usageBillingApiQueryService.currentUsage(workspaceId));
    }

    @GetMapping("/usage/monthly")
    @Operation(summary = "Get monthly workspace usage")
    public ApiResponse<WorkspaceUsageSummaryView> getMonthlyUsage(
            @PathVariable UUID workspaceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month
    ) {
        return ApiResponse.success(usageBillingApiQueryService.monthlyUsage(workspaceId, month));
    }

    @GetMapping("/usage-summary")
    @Operation(summary = "Get workspace usage summary")
    public ApiResponse<WorkspaceUsageSummaryView> getUsageSummary(
            @PathVariable UUID workspaceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month
    ) {
        return ApiResponse.success(usageBillingApiQueryService.monthlyUsage(workspaceId, month));
    }

    @GetMapping("/usage/snapshots")
    @Operation(summary = "List previous monthly usage snapshots")
    public ApiResponse<List<MonthlyUsageSnapshotView>> getUsageSnapshots(@PathVariable UUID workspaceId) {
        return ApiResponse.success(usageBillingApiQueryService.snapshots(workspaceId));
    }

    @GetMapping("/monthly-usage-snapshots")
    @Operation(summary = "List monthly usage snapshots")
    public ApiResponse<List<MonthlyUsageSnapshotView>> getMonthlyUsageSnapshots(@PathVariable UUID workspaceId) {
        return ApiResponse.success(usageBillingApiQueryService.snapshots(workspaceId));
    }

    @GetMapping("/credits/ledger")
    @Operation(summary = "List workspace credit ledger entries")
    public ApiResponse<List<CreditLedgerView>> getCreditLedger(@PathVariable UUID workspaceId) {
        return ApiResponse.success(usageBillingApiQueryService.creditLedger(workspaceId));
    }

    @GetMapping("/credit-ledger")
    @Operation(summary = "List paginated workspace credit ledger entries")
    public ApiResponse<PagedResult<CreditLedgerView>> getCreditLedger(
            @PathVariable UUID workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(usageBillingApiQueryService.creditLedger(workspaceId, pageable(page, size)));
    }

    @GetMapping("/billing/logs")
    @Operation(summary = "List workspace usage billing logs")
    public ApiResponse<List<UsageBillingLogView>> getBillingLogs(@PathVariable UUID workspaceId) {
        return ApiResponse.success(usageBillingApiQueryService.billingLogs(workspaceId));
    }

    @GetMapping("/usage-billing-logs")
    @Operation(summary = "List paginated workspace usage billing logs")
    public ApiResponse<PagedResult<UsageBillingLogView>> getUsageBillingLogs(
            @PathVariable UUID workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(usageBillingApiQueryService.billingLogs(workspaceId, pageable(page, size)));
    }

    @GetMapping("/download-usage")
    @Operation(summary = "List paginated workspace download usage")
    public ApiResponse<PagedResult<DownloadUsageView>> getDownloadUsage(
            @PathVariable UUID workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(usageBillingApiQueryService.downloadUsage(workspaceId, pageable(page, size)));
    }

    @GetMapping("/share-usage")
    @Operation(summary = "List paginated workspace share usage")
    public ApiResponse<PagedResult<ShareUsageView>> getShareUsage(
            @PathVariable UUID workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(usageBillingApiQueryService.shareUsage(workspaceId, pageable(page, size)));
    }

    @GetMapping("/usage/plan-utilization")
    @Operation(summary = "Get workspace plan utilization")
    public ApiResponse<PlanUtilizationReportView> getPlanUtilization(
            @PathVariable UUID workspaceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month
    ) {
        return ApiResponse.success(usageBillingApiQueryService.planUtilization(workspaceId, month));
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 100)));
    }
}
