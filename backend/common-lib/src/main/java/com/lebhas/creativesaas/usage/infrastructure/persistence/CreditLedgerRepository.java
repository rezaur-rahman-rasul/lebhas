package com.lebhas.creativesaas.usage.infrastructure.persistence;

import com.lebhas.creativesaas.usage.domain.CreditLedger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CreditLedgerRepository extends JpaRepository<CreditLedger, UUID> {

    List<CreditLedger> findAllByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);

    List<CreditLedger> findAllByReferenceTypeAndReferenceId(String referenceType, UUID referenceId);

    List<CreditLedger> findAllByWorkspaceIdAndReferenceTypeAndReferenceId(UUID workspaceId, String referenceType, UUID referenceId);
}
