package com.lebhas.ai.application;

import com.lebhas.ai.application.dto.WorkspaceAiUsageRecord;
import com.lebhas.ai.application.dto.WorkspaceAiUsageView;
import com.lebhas.ai.domain.WorkspaceAiUsage;
import com.lebhas.ai.infrastructure.persistence.WorkspaceAiUsageRepository;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.workspace.infrastructure.persistence.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class WorkspaceAiUsageService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceAiUsageRepository workspaceAiUsageRepository;
    private final WorkspaceAiUsageAggregator workspaceAiUsageAggregator;
    private final AiUsageAnalyticsMapper mapper;

    public WorkspaceAiUsageService(
            WorkspaceRepository workspaceRepository,
            WorkspaceAiUsageRepository workspaceAiUsageRepository,
            WorkspaceAiUsageAggregator workspaceAiUsageAggregator,
            AiUsageAnalyticsMapper mapper
    ) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceAiUsageRepository = workspaceAiUsageRepository;
        this.workspaceAiUsageAggregator = workspaceAiUsageAggregator;
        this.mapper = mapper;
    }

    @Transactional
    public WorkspaceAiUsageView recordUsage(WorkspaceAiUsageRecord record) {
        UUID workspaceId = requireWorkspace(record.workspaceId());
        WorkspaceAiUsage usage = workspaceAiUsageRepository.findByWorkspaceIdAndDeletedFalse(workspaceId)
                .orElseGet(() -> WorkspaceAiUsage.create(workspaceId));
        workspaceAiUsageAggregator.apply(usage, record);
        return mapper.toView(workspaceAiUsageRepository.save(usage));
    }

    @Transactional
    public WorkspaceAiUsageView recordGenerationRequested(UUID workspaceId) {
        return recordUsage(new WorkspaceAiUsageRecord(
                workspaceId,
                1,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                null));
    }

    @Transactional
    public WorkspaceAiUsageView recordGeneratedVersion(
            UUID workspaceId,
            BigDecimal creditsConsumed,
            BigDecimal estimatedCostUsd,
            BigDecimal generationTimeMs
    ) {
        return recordUsage(new WorkspaceAiUsageRecord(
                workspaceId,
                0,
                1,
                creditsConsumed,
                estimatedCostUsd,
                0,
                generationTimeMs));
    }

    @Transactional
    public WorkspaceAiUsageView recordGenerationFailure(UUID workspaceId, BigDecimal generationTimeMs) {
        return recordGenerationFailure(workspaceId, BigDecimal.ZERO, generationTimeMs);
    }

    @Transactional
    public WorkspaceAiUsageView recordGenerationFailure(
            UUID workspaceId,
            BigDecimal estimatedCostUsd,
            BigDecimal generationTimeMs
    ) {
        return recordUsage(new WorkspaceAiUsageRecord(
                workspaceId,
                0,
                0,
                BigDecimal.ZERO,
                estimatedCostUsd,
                1,
                generationTimeMs));
    }

    @Transactional
    public WorkspaceAiUsageView recordGenerationCompleted(
            UUID workspaceId,
            long generatedVersions,
            BigDecimal creditsConsumed,
            BigDecimal estimatedCostUsd,
            BigDecimal generationTimeMs
    ) {
        return recordUsage(new WorkspaceAiUsageRecord(
                workspaceId,
                0,
                generatedVersions,
                creditsConsumed,
                estimatedCostUsd,
                0,
                generationTimeMs));
    }

    private UUID requireWorkspace(UUID workspaceId) {
        if (workspaceId == null) {
            throw new BusinessException(ErrorCode.WORKSPACE_CONTEXT_REQUIRED);
        }
        return workspaceRepository.findByIdAndDeletedFalse(workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND))
                .getId();
    }
}
