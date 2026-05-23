package com.lebhas.ai.infrastructure.persistence;

import com.lebhas.ai.domain.AiLayerAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiLayerAnalyticsRepository extends JpaRepository<AiLayerAnalytics, UUID> {

    Optional<AiLayerAnalytics> findByLayerIdAndProviderIdAndModelNameAndDeletedFalse(UUID layerId, UUID providerId, String modelName);

    List<AiLayerAnalytics> findAllByLayerIdAndDeletedFalse(UUID layerId);

    List<AiLayerAnalytics> findAllByProviderIdAndDeletedFalse(UUID providerId);
}
