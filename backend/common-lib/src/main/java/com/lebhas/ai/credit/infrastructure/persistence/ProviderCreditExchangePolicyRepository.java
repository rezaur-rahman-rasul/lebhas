package com.lebhas.ai.credit.infrastructure.persistence;

import com.lebhas.ai.credit.domain.ProviderCreditExchangePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProviderCreditExchangePolicyRepository extends JpaRepository<ProviderCreditExchangePolicy, UUID> {

    Optional<ProviderCreditExchangePolicy> findFirstByProviderIdAndActiveTrueAndDeletedFalseOrderByUpdatedAtDesc(UUID providerId);

    Optional<ProviderCreditExchangePolicy> findFirstByProviderIdAndDeletedFalseOrderByUpdatedAtDesc(UUID providerId);
}
