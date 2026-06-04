package com.lebhas.ai.credit.infrastructure.persistence;

import com.lebhas.ai.credit.domain.ProviderCreditLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProviderCreditLedgerRepository extends JpaRepository<ProviderCreditLedger, UUID> {

    Page<ProviderCreditLedger> findAllByProviderIdOrderByCreatedAtDesc(UUID providerId, Pageable pageable);

    List<ProviderCreditLedger> findAllByReferenceTypeAndReferenceId(String referenceType, UUID referenceId);
}
