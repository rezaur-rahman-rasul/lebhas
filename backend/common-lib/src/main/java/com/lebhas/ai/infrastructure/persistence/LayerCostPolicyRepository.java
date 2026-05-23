package com.lebhas.ai.infrastructure.persistence;

import com.lebhas.ai.domain.LayerCostPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LayerCostPolicyRepository extends JpaRepository<LayerCostPolicy, UUID> {

    List<LayerCostPolicy> findAllByPipelineLayerIdAndDeletedFalseOrderByPriorityOrderAsc(UUID pipelineLayerId);

    Optional<LayerCostPolicy> findByPipelineLayerIdAndPolicyCodeAndDeletedFalse(UUID pipelineLayerId, String policyCode);
}
