package com.lebhas.creativesaas.generation.infrastructure.persistence;

import com.lebhas.creativesaas.generation.domain.CreativePipelineLayerRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CreativePipelineLayerRunRepository extends JpaRepository<CreativePipelineLayerRunEntity, UUID> {

    List<CreativePipelineLayerRunEntity> findAllByPipelineRunIdOrderBySequenceAsc(UUID pipelineRunId);
}
