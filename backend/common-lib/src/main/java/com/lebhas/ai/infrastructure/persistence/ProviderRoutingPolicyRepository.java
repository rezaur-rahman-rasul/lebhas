package com.lebhas.ai.infrastructure.persistence;

import com.lebhas.ai.domain.ProviderRoutingPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProviderRoutingPolicyRepository extends JpaRepository<ProviderRoutingPolicy, UUID> {

    Optional<ProviderRoutingPolicy> findByPolicyCodeAndDeletedFalse(String policyCode);

    Optional<ProviderRoutingPolicy> findByIdAndDeletedFalse(UUID id);

    List<ProviderRoutingPolicy> findAllByDeletedFalseOrderByPriorityOrderAscPolicyCodeAsc();

    List<ProviderRoutingPolicy> findAllByToolIdAndQualityModeAndEnabledTrueAndDeletedFalseOrderByPriorityOrderAsc(UUID toolId, String qualityMode);
}
