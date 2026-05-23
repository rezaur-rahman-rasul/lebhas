package com.lebhas.ai.infrastructure.persistence;

import com.lebhas.ai.domain.CreativePipeline;
import com.lebhas.ai.domain.CreativePipelineStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreativePipelineRepository extends JpaRepository<CreativePipeline, UUID> {

    Optional<CreativePipeline> findByPipelineCodeAndDeletedFalse(String pipelineCode);

    Optional<CreativePipeline> findFirstByActiveTrueAndStatusAndDeletedFalse(CreativePipelineStatus status);

    boolean existsByPipelineCodeAndDeletedFalse(String pipelineCode);

    List<CreativePipeline> findAllByDeletedFalseOrderByPipelineNameAscVersionDesc();
}
