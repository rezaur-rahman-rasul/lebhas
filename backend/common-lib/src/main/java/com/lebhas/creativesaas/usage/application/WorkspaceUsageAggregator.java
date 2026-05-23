package com.lebhas.creativesaas.usage.application;

import com.lebhas.creativesaas.storage.infrastructure.persistence.StorageUsageRepository;
import com.lebhas.creativesaas.usage.domain.WorkspaceUsageSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class WorkspaceUsageAggregator {

    private final WorkspaceUsageSummaryService workspaceUsageSummaryService;
    private final StorageUsageRepository storageUsageRepository;

    public WorkspaceUsageAggregator(
            WorkspaceUsageSummaryService workspaceUsageSummaryService,
            StorageUsageRepository storageUsageRepository
    ) {
        this.workspaceUsageSummaryService = workspaceUsageSummaryService;
        this.storageUsageRepository = storageUsageRepository;
    }

    @Transactional
    public WorkspaceUsageSummary aggregateMonth(UUID workspaceId, LocalDate usageMonth) {
        WorkspaceUsageSummary summary = workspaceUsageSummaryService.getOrCreateSummary(workspaceId, usageMonth);
        storageUsageRepository.findByWorkspaceIdAndDeletedFalse(workspaceId)
                .ifPresent(storage -> summary.recordStorageBytes(storage.getTotalUsedBytes()));
        return summary;
    }
}
