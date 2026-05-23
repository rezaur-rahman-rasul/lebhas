package com.lebhas.creativesaas.brand.infrastructure.persistence;

import com.lebhas.creativesaas.brand.domain.BrandEntity;
import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BrandRepository extends TenantAwareRepository<BrandEntity> {

    List<BrandEntity> findAllByWorkspaceIdAndDeletedFalseOrderByCreatedAtDesc(UUID workspaceId);

    Optional<BrandEntity> findFirstByWorkspaceIdAndDeletedFalseOrderByCreatedAtAsc(UUID workspaceId);
}
