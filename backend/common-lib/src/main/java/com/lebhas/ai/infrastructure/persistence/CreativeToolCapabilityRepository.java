package com.lebhas.ai.infrastructure.persistence;

import com.lebhas.ai.domain.CreativeToolCapability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CreativeToolCapabilityRepository extends JpaRepository<CreativeToolCapability, UUID> {

    List<CreativeToolCapability> findAllByToolIdAndDeletedFalseOrderByCapabilityCodeAsc(UUID toolId);
}
