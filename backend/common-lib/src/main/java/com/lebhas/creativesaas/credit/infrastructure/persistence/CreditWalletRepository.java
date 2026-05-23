package com.lebhas.creativesaas.credit.infrastructure.persistence;

import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;
import com.lebhas.creativesaas.credit.domain.CreditWalletEntity;

import java.util.Optional;
import java.util.UUID;

public interface CreditWalletRepository extends TenantAwareRepository<CreditWalletEntity> {

    Optional<CreditWalletEntity> findByWorkspaceIdAndDeletedFalse(UUID workspaceId);
}
