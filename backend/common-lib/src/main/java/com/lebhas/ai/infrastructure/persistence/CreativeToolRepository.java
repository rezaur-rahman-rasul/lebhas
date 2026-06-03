package com.lebhas.ai.infrastructure.persistence;

import com.lebhas.ai.domain.CreativeTool;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreativeToolRepository extends JpaRepository<CreativeTool, UUID> {

    Optional<CreativeTool> findByToolCodeAndDeletedFalse(String toolCode);

    Optional<CreativeTool> findByIdAndDeletedFalse(UUID id);

    boolean existsByToolCodeAndDeletedFalse(String toolCode);

    List<CreativeTool> findAllByDeletedFalseOrderByToolNameAsc();
}
