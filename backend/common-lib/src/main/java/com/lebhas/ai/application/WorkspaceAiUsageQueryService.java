package com.lebhas.ai.application;

import com.lebhas.ai.application.dto.WorkspaceAiUsageView;
import com.lebhas.ai.domain.WorkspaceAiUsage;
import com.lebhas.ai.infrastructure.persistence.WorkspaceAiUsageRepository;
import com.lebhas.creativesaas.pricing.application.WorkspacePlanContextService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class WorkspaceAiUsageQueryService {

    private final WorkspacePlanContextService workspacePlanContextService;
    private final WorkspaceAiUsageRepository workspaceAiUsageRepository;
    private final AiUsageAnalyticsMapper mapper;

    public WorkspaceAiUsageQueryService(
            WorkspacePlanContextService workspacePlanContextService,
            WorkspaceAiUsageRepository workspaceAiUsageRepository,
            AiUsageAnalyticsMapper mapper
    ) {
        this.workspacePlanContextService = workspacePlanContextService;
        this.workspaceAiUsageRepository = workspaceAiUsageRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public WorkspaceAiUsageView getWorkspaceUsage(UUID workspaceId) {
        UUID isolatedWorkspaceId = workspacePlanContextService.getWorkspacePlanContext(workspaceId).workspaceId();
        WorkspaceAiUsage usage = workspaceAiUsageRepository.findByWorkspaceIdAndDeletedFalse(isolatedWorkspaceId)
                .orElseGet(() -> WorkspaceAiUsage.create(isolatedWorkspaceId));
        return mapper.toView(usage);
    }
}
