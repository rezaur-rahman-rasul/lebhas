package com.lebhas.creativesaas.usage.infrastructure.persistence;

import com.lebhas.creativesaas.usage.domain.WorkspaceUsageSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceUsageSummaryRepository extends JpaRepository<WorkspaceUsageSummary, UUID> {

    Optional<WorkspaceUsageSummary> findByWorkspaceIdAndUsageMonth(UUID workspaceId, LocalDate usageMonth);

    List<WorkspaceUsageSummary> findAllByWorkspaceIdOrderByUsageMonthDesc(UUID workspaceId);

    Page<WorkspaceUsageSummary> findAllByUsageMonthOrderByUpdatedAtDesc(LocalDate usageMonth, Pageable pageable);

    Page<WorkspaceUsageSummary> findAllByUsageMonthOrderByTotalAiCostUsdDesc(LocalDate usageMonth, Pageable pageable);
}
