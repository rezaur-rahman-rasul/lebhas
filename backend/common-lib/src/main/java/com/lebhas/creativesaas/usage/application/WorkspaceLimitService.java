package com.lebhas.creativesaas.usage.application;

import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.generatedversion.infrastructure.persistence.GeneratedVersionRepository;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipStatus;
import com.lebhas.creativesaas.identity.infrastructure.persistence.WorkspaceMembershipRepository;
import com.lebhas.creativesaas.storage.infrastructure.persistence.StorageUsageRepository;
import com.lebhas.creativesaas.usage.domain.WorkspaceUsageSummary;
import com.lebhas.creativesaas.usage.infrastructure.persistence.WorkspaceUsageSummaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class WorkspaceLimitService {

    private static final BigDecimal BYTES_PER_GB = new BigDecimal("1073741824");

    private final WorkspaceUsageSummaryRepository workspaceUsageSummaryRepository;
    private final StorageUsageRepository storageUsageRepository;
    private final GeneratedVersionRepository generatedVersionRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;

    public WorkspaceLimitService(
            WorkspaceUsageSummaryRepository workspaceUsageSummaryRepository,
            StorageUsageRepository storageUsageRepository,
            GeneratedVersionRepository generatedVersionRepository,
            WorkspaceMembershipRepository workspaceMembershipRepository
    ) {
        this.workspaceUsageSummaryRepository = workspaceUsageSummaryRepository;
        this.storageUsageRepository = storageUsageRepository;
        this.generatedVersionRepository = generatedVersionRepository;
        this.workspaceMembershipRepository = workspaceMembershipRepository;
    }

    @Transactional(readOnly = true)
    public BigDecimal usedCreditsThisMonth(UUID workspaceId) {
        return currentSummary(workspaceId).map(WorkspaceUsageSummary::getUsedCredits).orElse(zero(4));
    }

    @Transactional(readOnly = true)
    public BigDecimal reservedCreditsThisMonth(UUID workspaceId) {
        return currentSummary(workspaceId).map(WorkspaceUsageSummary::getReservedCredits).orElse(zero(4));
    }

    @Transactional(readOnly = true)
    public long generatedVersionsForRequest(UUID workspaceId, UUID creativeRequestId) {
        return generatedVersionRepository.countByWorkspaceIdAndCreativeRequestIdAndDeletedFalse(workspaceId, creativeRequestId);
    }

    @Transactional(readOnly = true)
    public BigDecimal storageUsedGb(UUID workspaceId) {
        long bytes = storageUsageRepository.findByWorkspaceIdAndDeletedFalse(workspaceId)
                .map(storage -> Math.max(storage.getTotalUsedBytes(), 0L))
                .orElse(0L);
        return BigDecimal.valueOf(bytes).divide(BYTES_PER_GB, 4, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public long activeTeamMembers(UUID workspaceId) {
        return workspaceMembershipRepository.countByWorkspaceIdAndStatusAndDeletedFalse(
                workspaceId,
                WorkspaceMembershipStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public long activeCrewMembers(UUID workspaceId) {
        return workspaceMembershipRepository.findAllByWorkspaceIdAndDeletedFalse(workspaceId)
                .stream()
                .filter(membership -> membership.getStatus() == WorkspaceMembershipStatus.ACTIVE)
                .filter(membership -> membership.getRole() == Role.CREW)
                .count();
    }

    private java.util.Optional<WorkspaceUsageSummary> currentSummary(UUID workspaceId) {
        return workspaceUsageSummaryRepository.findByWorkspaceIdAndUsageMonth(workspaceId, currentMonth());
    }

    private LocalDate currentMonth() {
        return LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
    }

    private BigDecimal zero(int scale) {
        return BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
    }
}
