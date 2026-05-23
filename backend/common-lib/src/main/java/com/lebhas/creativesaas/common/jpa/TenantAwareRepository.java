package com.lebhas.creativesaas.common.jpa;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import com.lebhas.creativesaas.common.tenant.TenantContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NoRepositoryBean
public interface TenantAwareRepository<T extends TenantAwareEntity> extends JpaRepository<T, UUID> {

    Optional<T> findByIdAndWorkspaceIdAndDeletedFalse(UUID id, UUID workspaceId);

    List<T> findAllByWorkspaceIdAndDeletedFalse(UUID workspaceId);

    default Optional<T> findCurrentTenantById(UUID id) {
        return findByIdAndWorkspaceIdAndDeletedFalse(id, TenantContext.requireWorkspaceId());
    }

    default List<T> findAllForCurrentTenant() {
        return findAllByWorkspaceIdAndDeletedFalse(TenantContext.requireWorkspaceId());
    }
}
