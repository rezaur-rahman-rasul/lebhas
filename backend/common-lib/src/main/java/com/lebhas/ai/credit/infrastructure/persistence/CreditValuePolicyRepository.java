package com.lebhas.ai.credit.infrastructure.persistence;

import com.lebhas.ai.credit.domain.CreditValuePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CreditValuePolicyRepository extends JpaRepository<CreditValuePolicy, UUID> {

    Optional<CreditValuePolicy> findFirstByActiveTrueAndDeletedFalseOrderByEffectiveFromDescUpdatedAtDesc();

    Optional<CreditValuePolicy> findFirstByDeletedFalseOrderByUpdatedAtDesc();
}
