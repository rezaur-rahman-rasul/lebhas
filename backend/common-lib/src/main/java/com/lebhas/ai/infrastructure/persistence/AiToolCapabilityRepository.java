package com.lebhas.ai.infrastructure.persistence;

import com.lebhas.ai.domain.AiToolCapability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiToolCapabilityRepository extends JpaRepository<AiToolCapability, UUID> {

    List<AiToolCapability> findAllByProviderIdAndDeletedFalseOrderByLayerCodeAscCapabilityCodeAsc(UUID providerId);
}
