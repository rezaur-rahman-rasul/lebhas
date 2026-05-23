package com.lebhas.creativesaas.sharing.infrastructure.persistence;

import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;
import com.lebhas.creativesaas.sharing.domain.PublicShareLinkEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Deprecated(forRemoval = true)
public interface PublicShareLinkRepository extends TenantAwareRepository<PublicShareLinkEntity> {

    Optional<PublicShareLinkEntity> findFirstByGeneratedVersionIdAndDeletedFalse(UUID generatedVersionId);

    Optional<PublicShareLinkEntity> findByTokenAndDeletedFalse(String token);

    List<PublicShareLinkEntity> findAllByWorkspaceIdAndGeneratedVersionIdAndDeletedFalse(UUID workspaceId, UUID generatedVersionId);
}
