package com.lebhas.creativesaas.usage.infrastructure.persistence;

import com.lebhas.creativesaas.usage.domain.MonthlyUsageSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MonthlyUsageSnapshotRepository extends JpaRepository<MonthlyUsageSnapshot, UUID> {

    Optional<MonthlyUsageSnapshot> findByWorkspaceIdAndUsageMonth(UUID workspaceId, LocalDate usageMonth);

    List<MonthlyUsageSnapshot> findAllByWorkspaceIdOrderByUsageMonthDesc(UUID workspaceId);

    List<MonthlyUsageSnapshot> findAllByUsageMonthOrderByCreatedAtDesc(LocalDate usageMonth);
}
