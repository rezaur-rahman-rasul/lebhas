package com.lebhas.creativesaas.identity.infrastructure.persistence;

import com.lebhas.creativesaas.identity.domain.OnboardingRewardPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OnboardingRewardPolicyRepository extends JpaRepository<OnboardingRewardPolicy, UUID> {

    Optional<OnboardingRewardPolicy> findFirstByActiveTrueAndDeletedFalseOrderByUpdatedAtDesc();

    Optional<OnboardingRewardPolicy> findFirstByDeletedFalseOrderByUpdatedAtDesc();
}
