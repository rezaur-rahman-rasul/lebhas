package com.lebhas.creativesaas.usage.application;

import com.lebhas.creativesaas.usage.application.dto.MonthlyUsageSnapshotView;
import com.lebhas.creativesaas.workspace.infrastructure.persistence.WorkspaceRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Component
public class MonthlyUsageSnapshotJob {

    private final WorkspaceRepository workspaceRepository;
    private final MonthlyUsageSnapshotService monthlyUsageSnapshotService;

    public MonthlyUsageSnapshotJob(
            WorkspaceRepository workspaceRepository,
            MonthlyUsageSnapshotService monthlyUsageSnapshotService
    ) {
        this.workspaceRepository = workspaceRepository;
        this.monthlyUsageSnapshotService = monthlyUsageSnapshotService;
    }

    public List<UUID> createPreviousMonthSnapshots() {
        LocalDate previousMonth = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).minusMonths(1);
        return workspaceRepository.findAllByDeletedFalseOrderByCreatedAtDesc()
                .stream()
                .map(workspace -> monthlyUsageSnapshotService.createSnapshot(workspace.getId(), previousMonth).workspaceId())
                .toList();
    }

    public MonthlyUsageSnapshotView createPreviousMonthSnapshot(UUID workspaceId) {
        LocalDate previousMonth = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).minusMonths(1);
        return monthlyUsageSnapshotService.createSnapshot(workspaceId, previousMonth);
    }
}
