package com.lebhas.creativesaas.usage.application;

import com.lebhas.creativesaas.redis.RedisLockService;
import com.lebhas.creativesaas.usage.cache.UsageBillingLockService;
import com.lebhas.creativesaas.usage.application.dto.MonthlyUsageSnapshotView;
import com.lebhas.creativesaas.usage.domain.MonthlyUsageSnapshot;
import com.lebhas.creativesaas.usage.domain.WorkspaceUsageSummary;
import com.lebhas.creativesaas.usage.event.UsageBillingEventProducer;
import com.lebhas.creativesaas.usage.event.UsageSnapshotCreatedEventDto;
import com.lebhas.creativesaas.usage.infrastructure.persistence.MonthlyUsageSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MonthlyUsageSnapshotService {

    private static final Duration SNAPSHOT_LOCK_TTL = Duration.ofMinutes(2);

    private final MonthlyUsageSnapshotRepository monthlyUsageSnapshotRepository;
    private final WorkspaceUsageAggregator workspaceUsageAggregator;
    private final PlanUsagePolicyResolver planUsagePolicyResolver;
    private final UsageBillingLockService usageBillingLockService;
    private final UsageSummaryMapper usageSummaryMapper;
    private final UsageBillingEventProducer usageBillingEventProducer;

    public MonthlyUsageSnapshotService(
            MonthlyUsageSnapshotRepository monthlyUsageSnapshotRepository,
            WorkspaceUsageAggregator workspaceUsageAggregator,
            PlanUsagePolicyResolver planUsagePolicyResolver,
            UsageBillingLockService usageBillingLockService,
            UsageSummaryMapper usageSummaryMapper,
            UsageBillingEventProducer usageBillingEventProducer
    ) {
        this.monthlyUsageSnapshotRepository = monthlyUsageSnapshotRepository;
        this.workspaceUsageAggregator = workspaceUsageAggregator;
        this.planUsagePolicyResolver = planUsagePolicyResolver;
        this.usageBillingLockService = usageBillingLockService;
        this.usageSummaryMapper = usageSummaryMapper;
        this.usageBillingEventProducer = usageBillingEventProducer;
    }

    @Transactional(readOnly = true)
    public Optional<MonthlyUsageSnapshotView> getSnapshot(UUID workspaceId, LocalDate usageMonth) {
        return monthlyUsageSnapshotRepository.findByWorkspaceIdAndUsageMonth(
                        require(workspaceId, "workspaceId"),
                        normalizeMonth(usageMonth))
                .map(usageSummaryMapper::toView);
    }

    @Transactional(readOnly = true)
    public List<MonthlyUsageSnapshotView> getPreviousMonthSnapshots(UUID workspaceId) {
        LocalDate currentMonth = currentMonth();
        return monthlyUsageSnapshotRepository.findAllByWorkspaceIdOrderByUsageMonthDesc(require(workspaceId, "workspaceId"))
                .stream()
                .filter(snapshot -> snapshot.getUsageMonth().isBefore(currentMonth))
                .map(usageSummaryMapper::toView)
                .toList();
    }

    @Transactional
    public MonthlyUsageSnapshotView createSnapshot(UUID workspaceId, LocalDate usageMonth) {
        UUID normalizedWorkspaceId = require(workspaceId, "workspaceId");
        LocalDate month = normalizeMonth(usageMonth);
        RedisLockService.RedisLockToken lockToken = usageBillingLockService.acquireMonthlySnapshotLock(normalizedWorkspaceId, month)
                .orElseThrow(() -> new IllegalStateException("Monthly usage snapshot is already being generated"));
        try {
            MonthlyUsageSnapshot snapshot = monthlyUsageSnapshotRepository.findByWorkspaceIdAndUsageMonth(normalizedWorkspaceId, month)
                    .orElseGet(() -> createSnapshotRecord(normalizedWorkspaceId, month));
            return usageSummaryMapper.toView(snapshot);
        } finally {
            usageBillingLockService.release(lockToken, normalizedWorkspaceId, month);
        }
    }

    private MonthlyUsageSnapshot createSnapshotRecord(UUID workspaceId, LocalDate usageMonth) {
        WorkspaceUsageSummary summary = workspaceUsageAggregator.aggregateMonth(workspaceId, usageMonth);
        PlanUsagePolicyResolver.PlanUsagePolicy policy = planUsagePolicyResolver.resolve(workspaceId);
        MonthlyUsageSnapshot snapshot = monthlyUsageSnapshotRepository.save(MonthlyUsageSnapshot.create(
                workspaceId,
                usageMonth,
                policy.subscription().getPricingPlanId(),
                policy.subscription().getId(),
                summary.getUsedCredits(),
                summary.getTotalGeneratedVersions(),
                summary.getTotalCreativeRequests(),
                summary.getTotalAiCostUsd(),
                summary.getTotalStorageBytes(),
                summary.getTotalDownloads(),
                summary.getTotalPublicShares()));
        usageBillingEventProducer.publishUsageSnapshotCreated(new UsageSnapshotCreatedEventDto(
                workspaceId,
                snapshot.getId(),
                snapshot.getUsageMonth(),
                snapshot.getPricingPlanId(),
                snapshot.getSubscriptionId(),
                java.time.Instant.now()));
        return snapshot;
    }

    private LocalDate currentMonth() {
        return LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
    }

    private LocalDate normalizeMonth(LocalDate usageMonth) {
        if (usageMonth == null) {
            return currentMonth().minusMonths(1);
        }
        return usageMonth.withDayOfMonth(1);
    }

    private <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }
}
