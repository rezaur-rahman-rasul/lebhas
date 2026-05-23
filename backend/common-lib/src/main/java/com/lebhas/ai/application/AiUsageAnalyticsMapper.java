package com.lebhas.ai.application;

import com.lebhas.ai.application.dto.WorkspaceAiUsageView;
import com.lebhas.ai.domain.WorkspaceAiUsage;
import org.springframework.stereotype.Component;

@Component
public class AiUsageAnalyticsMapper {

    public WorkspaceAiUsageView toView(WorkspaceAiUsage usage) {
        if (usage == null) {
            return null;
        }
        return new WorkspaceAiUsageView(
                usage.getId(),
                usage.getWorkspaceId(),
                usage.getTotalGenerationRequests(),
                usage.getTotalGeneratedVersions(),
                usage.getTotalCreditsConsumed(),
                usage.getTotalEstimatedCostUsd(),
                usage.getTotalFailures(),
                usage.getAvgGenerationTimeMs(),
                usage.getCreatedAt(),
                usage.getUpdatedAt());
    }
}
