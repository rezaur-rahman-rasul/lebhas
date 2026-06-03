package com.lebhas.ai.infrastructure.persistence;

import com.lebhas.ai.domain.ToolCreditCostPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ToolCreditCostPolicyRepository extends JpaRepository<ToolCreditCostPolicy, UUID> {

    List<ToolCreditCostPolicy> findAllByToolIdAndDeletedFalseOrderByPolicyCodeAsc(UUID toolId);

    Optional<ToolCreditCostPolicy> findFirstByToolIdAndEnabledTrueAndDeletedFalseOrderByUpdatedAtDesc(UUID toolId);
}
