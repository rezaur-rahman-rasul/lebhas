package com.lebhas.creativesaas.usage.application;

import com.lebhas.creativesaas.usage.application.dto.PlanUtilizationReportView;
import com.lebhas.creativesaas.usage.domain.WorkspaceUsageSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class PlanUtilizationReportService {

    private static final BigDecimal BYTES_PER_GB = new BigDecimal("1073741824");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final WorkspaceUsageAggregator workspaceUsageAggregator;
    private final PlanUsagePolicyResolver planUsagePolicyResolver;

    public PlanUtilizationReportService(
            WorkspaceUsageAggregator workspaceUsageAggregator,
            PlanUsagePolicyResolver planUsagePolicyResolver
    ) {
        this.workspaceUsageAggregator = workspaceUsageAggregator;
        this.planUsagePolicyResolver = planUsagePolicyResolver;
    }

    @Transactional
    public PlanUtilizationReportView buildReport(UUID workspaceId, LocalDate usageMonth) {
        LocalDate month = usageMonth == null ? LocalDate.now(java.time.ZoneOffset.UTC).withDayOfMonth(1) : usageMonth.withDayOfMonth(1);
        WorkspaceUsageSummary summary = workspaceUsageAggregator.aggregateMonth(workspaceId, month);
        PlanUsagePolicyResolver.PlanUsagePolicy policy = planUsagePolicyResolver.resolve(workspaceId);
        BigDecimal monthlyCreditLimit = policy.featurePolicy().getMonthlyCreditLimit();
        BigDecimal maxStorageGb = policy.featurePolicy().getMaxStorageGb();
        return new PlanUtilizationReportView(
                workspaceId,
                month,
                policy.subscription().getPricingPlanId(),
                policy.subscription().getId(),
                summary.getUsedCredits(),
                monthlyCreditLimit,
                utilization(summary.getUsedCredits(), monthlyCreditLimit),
                summary.getTotalGeneratedVersions(),
                policy.featurePolicy().getMaxGeneratedVersionsPerRequest(),
                summary.getTotalStorageBytes(),
                maxStorageGb,
                utilization(bytesToGb(summary.getTotalStorageBytes()), maxStorageGb),
                summary.getTotalDownloads(),
                summary.getTotalPublicShares());
    }

    private BigDecimal utilization(BigDecimal used, BigDecimal limit) {
        if (used == null || limit == null || limit.signum() <= 0) {
            return null;
        }
        return used.multiply(HUNDRED).divide(limit, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal bytesToGb(long bytes) {
        return BigDecimal.valueOf(Math.max(bytes, 0L)).divide(BYTES_PER_GB, 4, RoundingMode.HALF_UP);
    }
}
