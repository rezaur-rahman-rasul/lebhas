package com.lebhas.ai.infrastructure.persistence;

import com.lebhas.ai.domain.AiModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiModelRepository extends JpaRepository<AiModel, UUID> {

    Optional<AiModel> findByProviderIdAndModelCodeAndDeletedFalse(UUID providerId, String modelCode);

    Optional<AiModel> findByIdAndDeletedFalse(UUID id);

    Optional<AiModel> findByIdAndProviderIdAndDeletedFalse(UUID id, UUID providerId);

    boolean existsByProviderIdAndModelCodeAndDeletedFalse(UUID providerId, String modelCode);

    List<AiModel> findAllByProviderIdAndDeletedFalseOrderByModelNameAsc(UUID providerId);
}
