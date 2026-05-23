package com.lebhas.ai.infrastructure.persistence;

import com.lebhas.ai.domain.AiProviderMetrics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiProviderMetricsRepository extends JpaRepository<AiProviderMetrics, UUID> {

    Optional<AiProviderMetrics> findByProviderIdAndModelNameAndDeletedFalse(UUID providerId, String modelName);

    List<AiProviderMetrics> findAllByProviderIdAndDeletedFalse(UUID providerId);
}
