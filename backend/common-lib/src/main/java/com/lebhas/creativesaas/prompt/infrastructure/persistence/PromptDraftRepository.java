package com.lebhas.creativesaas.prompt.infrastructure.persistence;

import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;
import com.lebhas.creativesaas.prompt.domain.PromptDraftEntity;

import java.util.List;
import java.util.UUID;

public interface PromptDraftRepository extends TenantAwareRepository<PromptDraftEntity> {

    List<PromptDraftEntity> findAllByWorkspaceIdAndProjectIdAndDeletedFalseOrderByUpdatedAtDesc(UUID workspaceId, UUID projectId);
}
