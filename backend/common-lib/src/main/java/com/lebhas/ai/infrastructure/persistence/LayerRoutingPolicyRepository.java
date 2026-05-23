package com.lebhas.ai.infrastructure.persistence;

import com.lebhas.ai.domain.LayerRoutingPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LayerRoutingPolicyRepository extends JpaRepository<LayerRoutingPolicy, UUID> {

    List<LayerRoutingPolicy> findAllByPipelineLayerIdAndDeletedFalseOrderByPriorityOrderAsc(UUID pipelineLayerId);

    Optional<LayerRoutingPolicy> findByPipelineLayerIdAndPolicyCodeAndDeletedFalse(UUID pipelineLayerId, String policyCode);
}
