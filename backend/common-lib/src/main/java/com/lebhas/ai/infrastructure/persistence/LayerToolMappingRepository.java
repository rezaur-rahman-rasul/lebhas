package com.lebhas.ai.infrastructure.persistence;

import com.lebhas.ai.domain.LayerToolMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LayerToolMappingRepository extends JpaRepository<LayerToolMapping, UUID> {

    List<LayerToolMapping> findAllByPipelineLayerIdAndDeletedFalseOrderByPriorityOrderAsc(UUID pipelineLayerId);

    List<LayerToolMapping> findAllByProviderIdAndDeletedFalse(UUID providerId);

    Optional<LayerToolMapping> findByPipelineLayerIdAndMappingCodeAndDeletedFalse(UUID pipelineLayerId, String mappingCode);
}
