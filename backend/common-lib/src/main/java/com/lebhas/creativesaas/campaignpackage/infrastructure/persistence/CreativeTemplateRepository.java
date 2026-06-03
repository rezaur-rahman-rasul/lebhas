package com.lebhas.creativesaas.campaignpackage.infrastructure.persistence;

import com.lebhas.creativesaas.campaignpackage.domain.CreativeTemplate;
import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreativeTemplateRepository extends TenantAwareRepository<CreativeTemplate> {
    Optional<CreativeTemplate> findByIdAndDeletedFalse(UUID id);
    List<CreativeTemplate> findAllByWorkspaceIdAndDeletedFalseOrderByCreatedAtDesc(UUID workspaceId);
    List<CreativeTemplate> findAllByMasterTemplateTrueAndActiveTrueAndDeletedFalseOrderByCreatedAtDesc();
}
