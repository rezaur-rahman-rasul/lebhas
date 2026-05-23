package com.lebhas.creativesaas.usage.application;

import com.lebhas.creativesaas.usage.application.dto.MonthlyUsageSnapshotView;
import com.lebhas.creativesaas.usage.application.dto.WorkspaceUsageSummaryView;
import com.lebhas.creativesaas.usage.domain.MonthlyUsageSnapshot;
import com.lebhas.creativesaas.usage.domain.WorkspaceUsageSummary;
import org.springframework.stereotype.Component;

@Component
public class UsageSummaryMapper {

    public WorkspaceUsageSummaryView toView(WorkspaceUsageSummary summary) {
        return new WorkspaceUsageSummaryView(
                summary.getId(),
                summary.getWorkspaceId(),
                summary.getUsageMonth(),
                summary.getUsedCredits(),
                summary.getReservedCredits(),
                summary.getRefundedCredits(),
                summary.getTotalCreativeRequests(),
                summary.getTotalGeneratedVersions(),
                summary.getTotalLayerExecutions(),
                summary.getTotalAiCostUsd(),
                summary.getTotalUploads(),
                summary.getTotalStorageBytes(),
                summary.getTotalDownloads(),
                summary.getTotalPublicShares(),
                summary.getTotalPromptEnhancements(),
                summary.getTotalGenerationFailures(),
                summary.getTotalApiCalls(),
                summary.getUpdatedAt());
    }

    public MonthlyUsageSnapshotView toView(MonthlyUsageSnapshot snapshot) {
        return new MonthlyUsageSnapshotView(
                snapshot.getId(),
                snapshot.getWorkspaceId(),
                snapshot.getUsageMonth(),
                snapshot.getPricingPlanId(),
                snapshot.getSubscriptionId(),
                snapshot.getUsedCredits(),
                snapshot.getGeneratedVersions(),
                snapshot.getCreativeRequests(),
                snapshot.getAiCostUsd(),
                snapshot.getStorageBytes(),
                snapshot.getDownloads(),
                snapshot.getPublicShares(),
                snapshot.getCreatedAt());
    }
}
