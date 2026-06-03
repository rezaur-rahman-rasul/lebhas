package com.lebhas.creativesaas.usage.application;

import com.lebhas.creativesaas.common.api.PagedResult;
import com.lebhas.creativesaas.usage.application.dto.PlanUtilizationReportView;
import com.lebhas.creativesaas.usage.application.dto.UsageBillingLogView;
import com.lebhas.creativesaas.usage.application.dto.WorkspaceUsageSummaryView;
import com.lebhas.creativesaas.usage.infrastructure.persistence.WorkspaceUsageSummaryRepository;
import com.lebhas.pricing.WorkspaceSubscription;
import com.lebhas.pricing.WorkspaceSubscriptionRepository;
import com.lebhas.pricing.WorkspaceSubscriptionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class MasterUsageQueryService {

    private final WorkspaceUsageSummaryRepository workspaceUsageSummaryRepository;
    private final WorkspaceUsageAggregator workspaceUsageAggregator;
    private final UsageSummaryMapper usageSummaryMapper;
    private final UsageBillingQueryService usageBillingQueryService;
    private final PlanUtilizationReportService planUtilizationReportService;
    private final WorkspaceSubscriptionRepository workspaceSubscriptionRepository;

    public MasterUsageQueryService(
            WorkspaceUsageSummaryRepository workspaceUsageSummaryRepository,
            WorkspaceUsageAggregator workspaceUsageAggregator,
            UsageSummaryMapper usageSummaryMapper,
            UsageBillingQueryService usageBillingQueryService,
            PlanUtilizationReportService planUtilizationReportService,
            WorkspaceSubscriptionRepository workspaceSubscriptionRepository
    ) {
        this.workspaceUsageSummaryRepository = workspaceUsageSummaryRepository;
        this.workspaceUsageAggregator = workspaceUsageAggregator;
        this.usageSummaryMapper = usageSummaryMapper;
        this.usageBillingQueryService = usageBillingQueryService;
        this.planUtilizationReportService = planUtilizationReportService;
        this.workspaceSubscriptionRepository = workspaceSubscriptionRepository;
    }

    @Transactional(readOnly = true)
    public PagedResult<WorkspaceUsageSummaryView> workspaceUsage(LocalDate month, Pageable pageable) {
        return PagedResult.from(workspaceUsageSummaryRepository.findAllByUsageMonthOrderByUpdatedAtDesc(normalizeMonth(month), pageable)
                .map(usageSummaryMapper::toView));
    }

    @Transactional
    public WorkspaceUsageSummaryView workspaceUsage(UUID workspaceId, LocalDate month) {
        return usageSummaryMapper.toView(workspaceUsageAggregator.aggregateMonth(workspaceId, normalizeMonth(month)));
    }

    @Transactional(readOnly = true)
    public PagedResult<UsageBillingLogView> aiCosts(Pageable pageable) {
        return usageBillingQueryService.findAiCostBillingLogs(pageable);
    }

    @Transactional(readOnly = true)
    public PagedResult<WorkspaceUsageSummaryView> topCostWorkspaces(LocalDate month, Pageable pageable) {
        return PagedResult.from(workspaceUsageSummaryRepository.findAllByUsageMonthOrderByTotalAiCostUsdDesc(normalizeMonth(month), pageable)
                .map(usageSummaryMapper::toView));
    }

    @Transactional
    public List<PlanUtilizationReportView> planUtilization(LocalDate month) {
        LocalDate usageMonth = normalizeMonth(month);
        return Stream.concat(
                        workspaceSubscriptionRepository.findAllByStatusAndDeletedFalse(WorkspaceSubscriptionStatus.ACTIVE).stream(),
                        workspaceSubscriptionRepository.findAllByStatusAndDeletedFalse(WorkspaceSubscriptionStatus.TRIAL).stream())
                .filter(this::isActive)
                .sorted(Comparator.comparing(WorkspaceSubscription::getWorkspaceId))
                .map(subscription -> planUtilizationReportService.buildReport(subscription.getWorkspaceId(), usageMonth))
                .toList();
    }

    private boolean isActive(WorkspaceSubscription subscription) {
        Instant now = Instant.now();
        return subscription.getExpiresAt() == null || !subscription.getExpiresAt().isBefore(now);
    }

    private LocalDate normalizeMonth(LocalDate month) {
        return (month == null ? LocalDate.now(ZoneOffset.UTC) : month).withDayOfMonth(1);
    }
}
