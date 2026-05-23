package com.lebhas.ai.infrastructure.persistence;

import com.lebhas.ai.domain.CreativeLayerType;
import com.lebhas.ai.domain.CreativePipelineLayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreativePipelineLayerRepository extends JpaRepository<CreativePipelineLayer, UUID> {

    Optional<CreativePipelineLayer> findByPipelineIdAndLayerTypeAndDeletedFalse(UUID pipelineId, CreativeLayerType layerType);

    List<CreativePipelineLayer> findAllByPipelineIdAndDeletedFalseOrderBySortOrderAsc(UUID pipelineId);
}
