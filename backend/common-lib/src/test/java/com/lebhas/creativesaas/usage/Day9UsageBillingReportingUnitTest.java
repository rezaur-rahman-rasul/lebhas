package com.lebhas.creativesaas.usage;

import com.lebhas.creativesaas.common.api.PagedResult;
import com.lebhas.creativesaas.usage.application.CreditLedgerService;
import com.lebhas.creativesaas.usage.application.DownloadUsageMapper;
import com.lebhas.creativesaas.usage.application.MasterUsageQueryService;
import com.lebhas.creativesaas.usage.application.MonthlyUsageSnapshotService;
import com.lebhas.creativesaas.usage.application.PlanUtilizationReportService;
import com.lebhas.creativesaas.usage.application.ShareUsageMapper;
import com.lebhas.creativesaas.usage.application.UsageBillingAccessService;
import com.lebhas.creativesaas.usage.application.UsageBillingApiQueryService;
import com.lebhas.creativesaas.usage.application.UsageBillingQueryService;
import com.lebhas.creativesaas.usage.application.UsageSummaryMapper;
import com.lebhas.creativesaas.usage.application.WorkspaceUsageAggregator;
import com.lebhas.creativesaas.usage.application.WorkspaceUsageSummaryService;
import com.lebhas.creativesaas.usage.application.dto.CreditLedgerView;
import com.lebhas.creativesaas.usage.application.dto.DownloadUsageView;
import com.lebhas.creativesaas.usage.application.dto.PlanUtilizationReportView;
import com.lebhas.creativesaas.usage.application.dto.ShareUsageView;
import com.lebhas.creativesaas.usage.application.dto.UsageBillingLogView;
import com.lebhas.creativesaas.usage.application.dto.WorkspaceUsageSummaryView;
import com.lebhas.creativesaas.usage.domain.DownloadUsageLog;
import com.lebhas.creativesaas.usage.domain.ShareUsageLog;
import com.lebhas.creativesaas.usage.domain.WorkspaceUsageSummary;
import com.lebhas.creativesaas.usage.infrastructure.persistence.DownloadUsageLogRepository;
import com.lebhas.creativesaas.usage.infrastructure.persistence.ShareUsageLogRepository;
import com.lebhas.creativesaas.usage.infrastructure.persistence.WorkspaceUsageSummaryRepository;
import com.lebhas.pricing.WorkspaceSubscription;
import com.lebhas.pricing.WorkspaceSubscriptionRepository;
import com.lebhas.pricing.WorkspaceSubscriptionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Day9UsageBillingReportingUnitTest {

    private static final UUID WORKSPACE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_WORKSPACE_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID GENERATED_VERSION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ASSET_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID SHARE_LINK_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final LocalDate MAY_2026 = LocalDate.of(2026, 5, 1);

    @Test
    void usageSummaryReturnsCurrentMonthWithZeroUsageSafely() {
        UsageBillingAccessService accessService = mock(UsageBillingAccessService.class);
        WorkspaceUsageAggregator aggregator = mock(WorkspaceUsageAggregator.class);
        when(accessService.requireUsageBillingView(WORKSPACE_ID)).thenReturn(WORKSPACE_ID);
        when(aggregator.aggregateMonth(eq(WORKSPACE_ID), eq(null))).thenReturn(WorkspaceUsageSummary.create(WORKSPACE_ID, MAY_2026));

        WorkspaceUsageSummaryView view = apiQueryService(accessService, aggregator).currentUsage(WORKSPACE_ID);

        assertThat(view.workspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(view.usedCredits()).isEqualByComparingTo("0.0000");
        assertThat(view.totalDownloads()).isZero();
    }

    @Test
    void creditLedgerIsReturnedAsPagedResult() {
        UsageBillingAccessService accessService = mock(UsageBillingAccessService.class);
        CreditLedgerService creditLedgerService = mock(CreditLedgerService.class);
        Pageable pageable = PageRequest.of(0, 20);
        PagedResult<CreditLedgerView> expected = new PagedResult<>(List.of(), 0, 0, 0, 20, true, true);
        when(accessService.requireUsageBillingView(WORKSPACE_ID)).thenReturn(WORKSPACE_ID);
        when(creditLedgerService.findWorkspaceLedger(WORKSPACE_ID, pageable)).thenReturn(expected);

        UsageBillingApiQueryService service = apiQueryService(accessService, mock(WorkspaceUsageAggregator.class), creditLedgerService);

        assertThat(service.creditLedger(WORKSPACE_ID, pageable)).isSameAs(expected);
    }

    @Test
    void downloadUsageIsPagedAndWorkspaceScoped() {
        UsageBillingAccessService accessService = mock(UsageBillingAccessService.class);
        DownloadUsageLogRepository repository = mock(DownloadUsageLogRepository.class);
        Pageable pageable = PageRequest.of(0, 10);
        DownloadUsageLog log = DownloadUsageLog.create(WORKSPACE_ID, GENERATED_VERSION_ID, ASSET_ID, USER_ID, "GENERATED_VERSION", null, null);
        when(accessService.requireUsageBillingView(WORKSPACE_ID)).thenReturn(WORKSPACE_ID);
        when(repository.findAllByWorkspaceIdOrderByCreatedAtDesc(WORKSPACE_ID, pageable)).thenReturn(new PageImpl<>(List.of(log), pageable, 1));

        PagedResult<DownloadUsageView> result = apiQueryService(accessService, mock(WorkspaceUsageAggregator.class), repository, mock(ShareUsageLogRepository.class))
                .downloadUsage(WORKSPACE_ID, pageable);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).workspaceId()).isEqualTo(WORKSPACE_ID);
        verify(repository).findAllByWorkspaceIdOrderByCreatedAtDesc(WORKSPACE_ID, pageable);
    }

    @Test
    void shareUsageIncludesAccessCountFromUsageLogs() {
        UsageBillingAccessService accessService = mock(UsageBillingAccessService.class);
        ShareUsageLogRepository repository = mock(ShareUsageLogRepository.class);
        Pageable pageable = PageRequest.of(0, 10);
        ShareUsageLog log = ShareUsageLog.create(WORKSPACE_ID, SHARE_LINK_ID, GENERATED_VERSION_ID, USER_ID, null, null, null);
        when(accessService.requireUsageBillingView(WORKSPACE_ID)).thenReturn(WORKSPACE_ID);
        when(repository.findAllByWorkspaceIdOrderByCreatedAtDesc(WORKSPACE_ID, pageable)).thenReturn(new PageImpl<>(List.of(log), pageable, 1));
        when(repository.countByShareLinkId(SHARE_LINK_ID)).thenReturn(7L);

        PagedResult<ShareUsageView> result = apiQueryService(accessService, mock(WorkspaceUsageAggregator.class), mock(DownloadUsageLogRepository.class), repository)
                .shareUsage(WORKSPACE_ID, pageable);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).accessCount()).isEqualTo(7L);
    }

    @Test
    void masterWorkspaceUsageCanViewAllSummaries() {
        WorkspaceUsageSummaryRepository repository = mock(WorkspaceUsageSummaryRepository.class);
        Pageable pageable = PageRequest.of(0, 20);
        WorkspaceUsageSummary summary = WorkspaceUsageSummary.create(OTHER_WORKSPACE_ID, MAY_2026);
        when(repository.findAllByUsageMonthOrderByUpdatedAtDesc(MAY_2026, pageable)).thenReturn(new PageImpl<>(List.of(summary), pageable, 1));

        PagedResult<WorkspaceUsageSummaryView> result = masterService(repository).workspaceUsage(MAY_2026, pageable);

        assertThat(result.items()).singleElement().extracting(WorkspaceUsageSummaryView::workspaceId).isEqualTo(OTHER_WORKSPACE_ID);
    }

    @Test
    void masterPlanUtilizationUsesActiveSubscriptionsAndPolicyService() {
        WorkspaceSubscriptionRepository subscriptionRepository = mock(WorkspaceSubscriptionRepository.class);
        PlanUtilizationReportService planService = mock(PlanUtilizationReportService.class);
        WorkspaceSubscription active = WorkspaceSubscription.create(WORKSPACE_ID, UUID.randomUUID(), WorkspaceSubscriptionStatus.ACTIVE, Instant.now(), null, null, true);
        PlanUtilizationReportView expected = new PlanUtilizationReportView(
                WORKSPACE_ID,
                MAY_2026,
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.ZERO,
                BigDecimal.TEN,
                BigDecimal.ZERO,
                0,
                4,
                0,
                BigDecimal.TEN,
                BigDecimal.ZERO,
                0,
                0);
        when(subscriptionRepository.findAllByStatusAndDeletedFalse(WorkspaceSubscriptionStatus.ACTIVE)).thenReturn(List.of(active));
        when(subscriptionRepository.findAllByStatusAndDeletedFalse(WorkspaceSubscriptionStatus.TRIAL)).thenReturn(List.of());
        when(planService.buildReport(WORKSPACE_ID, MAY_2026)).thenReturn(expected);

        MasterUsageQueryService service = new MasterUsageQueryService(
                mock(WorkspaceUsageSummaryRepository.class),
                mock(WorkspaceUsageAggregator.class),
                new UsageSummaryMapper(),
                mock(UsageBillingQueryService.class),
                planService,
                subscriptionRepository);

        assertThat(service.planUtilization(MAY_2026)).containsExactly(expected);
        verify(planService).buildReport(WORKSPACE_ID, MAY_2026);
    }

    @Test
    void aiCostLogsArePaginatedForMasterOverview() {
        UsageBillingQueryService queryService = mock(UsageBillingQueryService.class);
        Pageable pageable = PageRequest.of(0, 20);
        PagedResult<UsageBillingLogView> expected = new PagedResult<>(List.of(), 0, 0, 0, 20, true, true);
        when(queryService.findAiCostBillingLogs(pageable)).thenReturn(expected);

        MasterUsageQueryService service = new MasterUsageQueryService(
                mock(WorkspaceUsageSummaryRepository.class),
                mock(WorkspaceUsageAggregator.class),
                new UsageSummaryMapper(),
                queryService,
                mock(PlanUtilizationReportService.class),
                mock(WorkspaceSubscriptionRepository.class));

        assertThat(service.aiCosts(pageable)).isSameAs(expected);
    }

    @Test
    void usageReportingDoesNotUseLocalFilesystemStorage() throws Exception {
        Pattern forbiddenFilesystemStorage = Pattern.compile("\\b(java\\.io\\.File|FileOutputStream|FileInputStream|Files\\.write|Files\\.copy)\\b");
        assertThat(sourceFilesWithMatch(forbiddenFilesystemStorage)).isEmpty();
    }

    @Test
    void usageReportingDoesNotHardcodePackageNames() throws Exception {
        Pattern forbiddenPlanNames = Pattern.compile("(?i)\\b(free|basic|pro|enterprise)\\b");
        assertThat(sourceFilesWithMatch(forbiddenPlanNames)).isEmpty();
    }

    private UsageBillingApiQueryService apiQueryService(UsageBillingAccessService accessService, WorkspaceUsageAggregator aggregator) {
        return apiQueryService(accessService, aggregator, mock(CreditLedgerService.class));
    }

    private UsageBillingApiQueryService apiQueryService(
            UsageBillingAccessService accessService,
            WorkspaceUsageAggregator aggregator,
            CreditLedgerService creditLedgerService
    ) {
        return new UsageBillingApiQueryService(
                accessService,
                mock(WorkspaceUsageSummaryService.class),
                aggregator,
                mock(MonthlyUsageSnapshotService.class),
                creditLedgerService,
                mock(UsageBillingQueryService.class),
                mock(PlanUtilizationReportService.class),
                new UsageSummaryMapper(),
                mock(DownloadUsageLogRepository.class),
                new DownloadUsageMapper(),
                mock(ShareUsageLogRepository.class),
                new ShareUsageMapper());
    }

    private UsageBillingApiQueryService apiQueryService(
            UsageBillingAccessService accessService,
            WorkspaceUsageAggregator aggregator,
            DownloadUsageLogRepository downloadRepository,
            ShareUsageLogRepository shareRepository
    ) {
        return new UsageBillingApiQueryService(
                accessService,
                mock(WorkspaceUsageSummaryService.class),
                aggregator,
                mock(MonthlyUsageSnapshotService.class),
                mock(CreditLedgerService.class),
                mock(UsageBillingQueryService.class),
                mock(PlanUtilizationReportService.class),
                new UsageSummaryMapper(),
                downloadRepository,
                new DownloadUsageMapper(),
                shareRepository,
                new ShareUsageMapper());
    }

    private MasterUsageQueryService masterService(WorkspaceUsageSummaryRepository repository) {
        return new MasterUsageQueryService(
                repository,
                mock(WorkspaceUsageAggregator.class),
                new UsageSummaryMapper(),
                mock(UsageBillingQueryService.class),
                mock(PlanUtilizationReportService.class),
                mock(WorkspaceSubscriptionRepository.class));
    }

    private List<String> sourceFilesWithMatch(Pattern pattern) throws Exception {
        try (var files = Files.walk(Path.of("src/main/java/com/lebhas/creativesaas/usage"))) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> contains(path, pattern))
                    .map(Path::toString)
                    .toList();
        }
    }

    private boolean contains(Path path, Pattern pattern) {
        try {
            return pattern.matcher(Files.readString(path, StandardCharsets.UTF_8)).find();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to inspect " + path, exception);
        }
    }
}
