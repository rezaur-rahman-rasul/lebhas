package com.lebhas.creativesaas.prompt.infrastructure.persistence;

import com.lebhas.creativesaas.prompt.domain.PromptEnhancementHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PromptEnhancementHistoryRepository extends JpaRepository<PromptEnhancementHistoryEntity, UUID> {

    List<PromptEnhancementHistoryEntity> findAllByCreativeRequestIdAndDeletedFalseOrderByCreatedAtDesc(UUID creativeRequestId);
}
