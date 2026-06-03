package com.lebhas.creativesaas.usage.application;

import com.lebhas.creativesaas.common.api.PagedResult;
import com.lebhas.creativesaas.usage.application.dto.CreditLedgerView;
import com.lebhas.creativesaas.usage.application.dto.DownloadUsageView;
import com.lebhas.creativesaas.usage.application.dto.MonthlyUsageSnapshotView;
import com.lebhas.creativesaas.usage.application.dto.PlanUtilizationReportView;
import com.lebhas.creativesaas.usage.application.dto.ShareUsageView;
import com.lebhas.creativesaas.usage.application.dto.UsageBillingLogView;
import com.lebhas.creativesaas.usage.application.dto.WorkspaceUsageSummaryView;
import com.lebhas.creativesaas.usage.infrastructure.persistence.DownloadUsageLogRepository;
import com.lebhas.creativesaas.usage.infrastructure.persistence.ShareUsageLogRepository;
import org.springframework.data.domain.Pageable;
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
    private final DownloadUsageLogRepository downloadUsageLogRepository;
    private final DownloadUsageMapper downloadUsageMapper;
    private final ShareUsageLogRepository shareUsageLogRepository;
    private final ShareUsageMapper shareUsageMapper;

    public UsageBillingApiQueryService(
            UsageBillingAccessService usageBillingAccessService,
            WorkspaceUsageSummaryService workspaceUsageSummaryService,
            WorkspaceUsageAggregator workspaceUsageAggregator,
            MonthlyUsageSnapshotService monthlyUsageSnapshotService,
            CreditLedgerService creditLedgerService,
            UsageBillingQueryService usageBillingQueryService,
            PlanUtilizationReportService planUtilizationReportService,
            UsageSummaryMapper usageSummaryMapper,
            DownloadUsageLogRepository downloadUsageLogRepository,
            DownloadUsageMapper downloadUsageMapper,
            ShareUsageLogRepository shareUsageLogRepository,
            ShareUsageMapper shareUsageMapper
    ) {
        this.usageBillingAccessService = usageBillingAccessService;
        this.workspaceUsageSummaryService = workspaceUsageSummaryService;
        this.workspaceUsageAggregator = workspaceUsageAggregator;
        this.monthlyUsageSnapshotService = monthlyUsageSnapshotService;
        this.creditLedgerService = creditLedgerService;
        this.usageBillingQueryService = usageBillingQueryService;
        this.planUtilizationReportService = planUtilizationReportService;
        this.usageSummaryMapper = usageSummaryMapper;
        this.downloadUsageLogRepository = downloadUsageLogRepository;
        this.downloadUsageMapper = downloadUsageMapper;
        this.shareUsageLogRepository = shareUsageLogRepository;
        this.shareUsageMapper = shareUsageMapper;
    }

    @Transactional
    public WorkspaceUsageSummaryView currentUsage(UUID workspaceId) {
        UUID authorizedWorkspaceId = usageBillingAccessService.requireUsageBillingView(workspaceId);
        return usageSummaryMapper.toView(workspaceUsageAggregator.aggregateMonth(authorizedWorkspaceId, null));
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
    public PagedResult<CreditLedgerView> creditLedger(UUID workspaceId, Pageable pageable) {
        UUID authorizedWorkspaceId = usageBillingAccessService.requireUsageBillingView(workspaceId);
        return creditLedgerService.findWorkspaceLedger(authorizedWorkspaceId, pageable);
    }

    @Transactional(readOnly = true)
    public List<UsageBillingLogView> billingLogs(UUID workspaceId) {
        UUID authorizedWorkspaceId = usageBillingAccessService.requireUsageBillingView(workspaceId);
        return usageBillingQueryService.findWorkspaceBillingLogs(authorizedWorkspaceId);
    }

    @Transactional(readOnly = true)
    public PagedResult<UsageBillingLogView> billingLogs(UUID workspaceId, Pageable pageable) {
        UUID authorizedWorkspaceId = usageBillingAccessService.requireUsageBillingView(workspaceId);
        return usageBillingQueryService.findWorkspaceBillingLogs(authorizedWorkspaceId, pageable);
    }

    @Transactional(readOnly = true)
    public PagedResult<DownloadUsageView> downloadUsage(UUID workspaceId, Pageable pageable) {
        UUID authorizedWorkspaceId = usageBillingAccessService.requireUsageBillingView(workspaceId);
        return PagedResult.from(downloadUsageLogRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(authorizedWorkspaceId, pageable)
                .map(downloadUsageMapper::toView));
    }

    @Transactional(readOnly = true)
    public PagedResult<ShareUsageView> shareUsage(UUID workspaceId, Pageable pageable) {
        UUID authorizedWorkspaceId = usageBillingAccessService.requireUsageBillingView(workspaceId);
        return PagedResult.from(shareUsageLogRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(authorizedWorkspaceId, pageable)
                .map(log -> shareUsageMapper.toView(log, shareUsageLogRepository.countByShareLinkId(log.getShareLinkId()))));
    }

    @Transactional
    public PlanUtilizationReportView planUtilization(UUID workspaceId, LocalDate month) {
        UUID authorizedWorkspaceId = usageBillingAccessService.requireUsageBillingView(workspaceId);
        return planUtilizationReportService.buildReport(authorizedWorkspaceId, month);
    }
}
