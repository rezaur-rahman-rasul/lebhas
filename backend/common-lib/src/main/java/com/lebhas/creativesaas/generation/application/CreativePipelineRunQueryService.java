package com.lebhas.creativesaas.generation.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.generation.application.dto.CreativePipelineLayerRunView;
import com.lebhas.creativesaas.generation.application.dto.CreativePipelineRunView;
import com.lebhas.creativesaas.generation.domain.CreativePipelineLayerRunEntity;
import com.lebhas.creativesaas.generation.domain.CreativePipelineRunEntity;
import com.lebhas.creativesaas.generation.infrastructure.persistence.CreativePipelineLayerRunRepository;
import com.lebhas.creativesaas.generation.infrastructure.persistence.CreativePipelineRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CreativePipelineRunQueryService {

    private final CreativePipelineRunRepository runRepository;
    private final CreativePipelineLayerRunRepository layerRunRepository;

    public CreativePipelineRunQueryService(
            CreativePipelineRunRepository runRepository,
            CreativePipelineLayerRunRepository layerRunRepository
    ) {
        this.runRepository = runRepository;
        this.layerRunRepository = layerRunRepository;
    }

    @Transactional(readOnly = true)
    public CreativePipelineRunView getLatestByCreativeRequest(UUID workspaceId, UUID creativeRequestId) {
        CreativePipelineRunEntity run = runRepository
                .findFirstByCreativeRequestIdAndWorkspaceIdAndDeletedFalseOrderByCreatedAtDesc(creativeRequestId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Creative pipeline run not found"));
        return toView(run);
    }

    CreativePipelineRunView toView(CreativePipelineRunEntity run) {
        return new CreativePipelineRunView(
                run.getCreativeRequestId(),
                run.getId(),
                run.getStatus(),
                run.getStrategy(),
                run.getPrimaryProviderCode(),
                run.getPlanJson(),
                run.getEstimatedCreditCost(),
                run.getActualCreditCost(),
                run.getFailureReason(),
                run.getCreatedAt(),
                run.getUpdatedAt(),
                run.getCompletedAt(),
                layerRunRepository.findAllByPipelineRunIdOrderBySequenceAsc(run.getId()).stream()
                        .map(this::toLayerView)
                        .toList());
    }

    private CreativePipelineLayerRunView toLayerView(CreativePipelineLayerRunEntity layer) {
        return new CreativePipelineLayerRunView(
                layer.getId(),
                layer.getSequence(),
                layer.getLayerType(),
                layer.getProviderCode(),
                layer.getModelCode(),
                layer.getStatus(),
                layer.getInputJson(),
                layer.getOutputJson(),
                layer.getInputAssetIds(),
                layer.getOutputAssetIds(),
                layer.getEstimatedCost(),
                layer.getActualCost(),
                layer.getStartedAt(),
                layer.getCompletedAt(),
                layer.getFailureReason());
    }
}
