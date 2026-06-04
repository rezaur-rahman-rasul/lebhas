package com.lebhas.ai.credit.infrastructure.persistence;

import com.lebhas.ai.credit.domain.ProviderCreditPool;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProviderCreditPoolRepository extends JpaRepository<ProviderCreditPool, UUID> {

    Optional<ProviderCreditPool> findFirstByProviderIdAndDeletedFalseOrderByUpdatedAtDesc(UUID providerId);

    List<ProviderCreditPool> findAllByDeletedFalseOrderByUpdatedAtDesc();
}
