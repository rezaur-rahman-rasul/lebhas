package com.lebhas.creativesaas.usage.application;

import com.lebhas.creativesaas.usage.application.dto.WorkspaceUsageSummaryView;
import com.lebhas.creativesaas.usage.cache.UsageMonthlyCounterService;
import com.lebhas.creativesaas.usage.cache.WorkspaceUsageSummaryCacheService;
import com.lebhas.creativesaas.usage.domain.WorkspaceUsageSummary;
import com.lebhas.creativesaas.usage.event.UsageBillingEventProducer;
import com.lebhas.creativesaas.usage.event.UsageUpdatedEventDto;
import com.lebhas.creativesaas.usage.infrastructure.persistence.WorkspaceUsageSummaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class WorkspaceUsageSummaryService {

    private final WorkspaceUsageSummaryRepository workspaceUsageSummaryRepository;
    private final UsageSummaryMapper usageSummaryMapper;
    private final WorkspaceUsageSummaryCacheService workspaceUsageSummaryCacheService;
    private final UsageMonthlyCounterService usageMonthlyCounterService;
    private final UsageBillingEventProducer usageBillingEventProducer;

    public WorkspaceUsageSummaryService(
            WorkspaceUsageSummaryRepository workspaceUsageSummaryRepository,
            UsageSummaryMapper usageSummaryMapper,
            WorkspaceUsageSummaryCacheService workspaceUsageSummaryCacheService,
            UsageMonthlyCounterService usageMonthlyCounterService,
            UsageBillingEventProducer usageBillingEventProducer
    ) {
        this.workspaceUsageSummaryRepository = workspaceUsageSummaryRepository;
        this.usageSummaryMapper = usageSummaryMapper;
        this.workspaceUsageSummaryCacheService = workspaceUsageSummaryCacheService;
        this.usageMonthlyCounterService = usageMonthlyCounterService;
        this.usageBillingEventProducer = usageBillingEventProducer;
    }

    @Transactional(readOnly = true)
    public Optional<WorkspaceUsageSummaryView> getCurrentMonthUsage(UUID workspaceId) {
        return getWorkspaceUsageSummary(workspaceId, currentMonth());
    }

    @Transactional(readOnly = true)
    public Optional<WorkspaceUsageSummaryView> getWorkspaceUsageSummary(UUID workspaceId, LocalDate usageMonth) {
        return workspaceUsageSummaryRepository.findByWorkspaceIdAndUsageMonth(
                        require(workspaceId, "workspaceId"),
                        normalizeMonth(usageMonth))
                .map(usageSummaryMapper::toView);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceUsageSummaryView> getWorkspaceUsageHistory(UUID workspaceId) {
        return workspaceUsageSummaryRepository.findAllByWorkspaceIdOrderByUsageMonthDesc(require(workspaceId, "workspaceId"))
                .stream()
                .map(usageSummaryMapper::toView)
                .toList();
    }

    @Transactional
    WorkspaceUsageSummary getOrCreateSummary(UUID workspaceId, LocalDate usageMonth) {
        LocalDate month = normalizeMonth(usageMonth);
        return workspaceUsageSummaryRepository.findByWorkspaceIdAndUsageMonth(require(workspaceId, "workspaceId"), month)
                .orElseGet(() -> workspaceUsageSummaryRepository.save(WorkspaceUsageSummary.create(workspaceId, month)));
    }

    void recordSummaryMutation(
            WorkspaceUsageSummary summary,
            String referenceType,
            UUID referenceId,
            String updateType
    ) {
        if (summary == null) {
            return;
        }
        workspaceUsageSummaryCacheService.invalidate(summary.getWorkspaceId(), summary.getUsageMonth());
        usageMonthlyCounterService.invalidate(summary.getWorkspaceId(), summary.getUsageMonth());
        usageBillingEventProducer.publishUsageUpdated(new UsageUpdatedEventDto(
                summary.getWorkspaceId(),
                summary.getUsageMonth(),
                referenceId,
                referenceType,
                updateType,
                Instant.now()));
    }

    private LocalDate currentMonth() {
        return LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
    }

    private LocalDate normalizeMonth(LocalDate usageMonth) {
        if (usageMonth == null) {
            return currentMonth();
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
