package com.lebhas.creativesaas.prompt.infrastructure.persistence;

import com.lebhas.creativesaas.prompt.domain.PromptTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplateEntity, UUID>, JpaSpecificationExecutor<PromptTemplateEntity> {

    Optional<PromptTemplateEntity> findByIdAndDeletedFalse(UUID id);

    Optional<PromptTemplateEntity> findByIdAndWorkspaceIdAndDeletedFalse(UUID id, UUID workspaceId);

    List<PromptTemplateEntity> findAllByIsPublicTrueAndDeletedFalseOrderByUpdatedAtDesc();

    List<PromptTemplateEntity> findAllByWorkspaceIdAndDeletedFalseOrderByUpdatedAtDesc(UUID workspaceId);

    List<PromptTemplateEntity> findAllByWorkspaceIdAndIsPublicTrueAndDeletedFalseOrderByUpdatedAtDesc(UUID workspaceId);

    List<PromptTemplateEntity> findAllByWorkspaceIdAndCategoryAndDeletedFalseOrderByUpdatedAtDesc(
            UUID workspaceId,
            String category
    );
}
