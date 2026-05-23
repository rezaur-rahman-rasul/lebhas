package com.lebhas.ai.infrastructure.persistence;

import com.lebhas.ai.domain.LayerQualityPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LayerQualityPolicyRepository extends JpaRepository<LayerQualityPolicy, UUID> {

    List<LayerQualityPolicy> findAllByPipelineLayerIdAndDeletedFalseOrderByPriorityOrderAsc(UUID pipelineLayerId);

    Optional<LayerQualityPolicy> findByPipelineLayerIdAndPolicyCodeAndDeletedFalse(UUID pipelineLayerId, String policyCode);
}
