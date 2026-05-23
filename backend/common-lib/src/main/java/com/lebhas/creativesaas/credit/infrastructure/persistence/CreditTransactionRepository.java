package com.lebhas.creativesaas.credit.infrastructure.persistence;

import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;
import com.lebhas.creativesaas.credit.domain.CreditTransactionEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreditTransactionRepository extends TenantAwareRepository<CreditTransactionEntity> {

    List<CreditTransactionEntity> findAllByWorkspaceIdAndDeletedFalseOrderByCreatedAtDesc(UUID workspaceId);

    List<CreditTransactionEntity> findAllByWorkspaceIdAndReferenceTypeAndReferenceIdAndDeletedFalseOrderByCreatedAtAsc(
            UUID workspaceId,
            String referenceType,
            UUID referenceId
    );

    Optional<CreditTransactionEntity> findFirstByWorkspaceIdAndReferenceTypeAndReferenceIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID workspaceId,
            String referenceType,
            UUID referenceId
    );
}
