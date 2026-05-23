package com.lebhas.pricing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlanFeaturePolicyRepository extends JpaRepository<PlanFeaturePolicy, UUID> {

    Optional<PlanFeaturePolicy> findByIdAndDeletedFalse(UUID id);

    Optional<PlanFeaturePolicy> findByPricingPlanIdAndDeletedFalse(UUID pricingPlanId);

    boolean existsByPricingPlanIdAndDeletedFalse(UUID pricingPlanId);
}
