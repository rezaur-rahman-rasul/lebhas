package com.lebhas.creativesaas.usage.application;

import com.lebhas.creativesaas.usage.application.dto.CreditLedgerView;
import com.lebhas.creativesaas.usage.application.dto.MonthlyUsageSnapshotView;
import com.lebhas.creativesaas.usage.application.dto.PlanUtilizationReportView;
import com.lebhas.creativesaas.usage.application.dto.UsageBillingLogView;
import com.lebhas.creativesaas.usage.application.dto.WorkspaceUsageSummaryView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class UsageBillingApiQueryService {

    private final UsageBillingAccessService usageBillingAccessService;
    private final WorkspaceUsageSummaryService workspaceUsageSummaryService;
    private final WorkspaceUsageAggregator workspaceUsageAggregator;
    private final MonthlyUsageSnapshotService monthlyUsageSnapshotService;
    private final CreditLedgerService creditLedgerService;
    private final UsageBillingQueryService usageBillingQueryService;
    private final PlanUtilizationReportService planUtilizationReportService;
    private final UsageSummaryMapper usageSummaryMapper;

    public UsageBillingApiQueryService(
            UsageBillingAccessService usageBillingAccessService,
            WorkspaceUsageSummaryService workspaceUsageSummaryService,
            WorkspaceUsageAggregator workspaceUsageAggregator,
            MonthlyUsageSnapshotService monthlyUsageSnapshotService,
            CreditLedgerService creditLedgerService,
            UsageBillingQueryService usageBillingQueryService,
            PlanUtilizationReportService planUtilizationReportService,
            UsageSummaryMapper usageSummaryMapper
    ) {
        this.usageBillingAccessService = usageBillingAccessService;
        this.workspaceUsageSummaryService = workspaceUsageSummaryService;
        this.workspaceUsageAggregator = workspaceUsageAggregator;
        this.monthlyUsageSnapshotService = monthlyUsageSnapshotService;
        this.creditLedgerService = creditLedgerService;
        this.usageBillingQueryService = usageBillingQueryService;
        this.planUtilizationReportService = planUtilizationReportService;
        this.usageSummaryMapper = usageSummaryMapper;
    }

    @Transactional(readOnly = true)
    public WorkspaceUsageSummaryView currentUsage(UUID workspaceId) {
        UUID authorizedWorkspaceId = usageBillingAccessService.requireUsageBillingView(workspaceId);
        return workspaceUsageSummaryService.getCurrentMonthUsage(authorizedWorkspaceId).orElse(null);
    }

    @Transactional
    public WorkspaceUsageSummaryView monthlyUsage(UUID workspaceId, LocalDate month) {
        UUID authorizedWorkspaceId = usageBillingAccessService.requireUsageBillingView(workspaceId);
        return usageSummaryMapper.toView(workspaceUsageAggregator.aggregateMonth(authorizedWorkspaceId, month));
    }

    @Transactional(readOnly = true)
    public List<MonthlyUsageSnapshotView> snapshots(UUID workspaceId) {
        UUID authorizedWorkspaceId = usageBillingAccessService.requireUsageBillingView(workspaceId);
        return monthlyUsageSnapshotService.getPreviousMonthSnapshots(authorizedWorkspaceId);
    }

    @Transactional(readOnly = true)
    public List<CreditLedgerView> creditLedger(UUID workspaceId) {
        UUID authorizedWorkspaceId = usageBillingAccessService.requireUsageBillingView(workspaceId);
        return creditLedgerService.findWorkspaceLedger(authorizedWorkspaceId);
    }

    @Transactional(readOnly = true)
    public List<UsageBillingLogView> billingLogs(UUID workspaceId) {
        UUID authorizedWorkspaceId = usageBillingAccessService.requireUsageBillingView(workspaceId);
        return usageBillingQueryService.findWorkspaceBillingLogs(authorizedWorkspaceId);
    }

    @Transactional
    public PlanUtilizationReportView planUtilization(UUID workspaceId, LocalDate month) {
        UUID authorizedWorkspaceId = usageBillingAccessService.requireUsageBillingView(workspaceId);
        return planUtilizationReportService.buildReport(authorizedWorkspaceId, month);
    }
}
