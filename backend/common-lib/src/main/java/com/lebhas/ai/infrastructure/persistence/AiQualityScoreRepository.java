package com.lebhas.ai.infrastructure.persistence;

import com.lebhas.ai.domain.AiQualityScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiQualityScoreRepository extends JpaRepository<AiQualityScore, UUID> {

    Optional<AiQualityScore> findByGeneratedVersionIdAndDeletedFalse(UUID generatedVersionId);

    List<AiQualityScore> findAllByWorkspaceIdAndDeletedFalse(UUID workspaceId);
}
