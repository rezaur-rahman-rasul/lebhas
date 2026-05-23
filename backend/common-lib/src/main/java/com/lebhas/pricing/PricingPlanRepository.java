package com.lebhas.pricing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PricingPlanRepository extends JpaRepository<PricingPlan, UUID> {

    Optional<PricingPlan> findByIdAndDeletedFalse(UUID id);

    Optional<PricingPlan> findByCodeIgnoreCaseAndDeletedFalse(String code);

    boolean existsByCodeIgnoreCaseAndDeletedFalse(String code);

    Optional<PricingPlan> findFirstByDefaultPlanTrueAndDeletedFalse();

    List<PricingPlan> findAllByDeletedFalseOrderBySortOrderAscNameAsc();

    List<PricingPlan> findAllByActiveTrueAndDeletedFalseOrderBySortOrderAscNameAsc();
}
