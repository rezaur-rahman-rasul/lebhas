package com.lebhas.creativesaas.operations.infrastructure.persistence;

import com.lebhas.creativesaas.operations.domain.SystemFeatureToggle;
import com.lebhas.creativesaas.operations.domain.SystemFeatureToggleKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SystemFeatureToggleRepository extends JpaRepository<SystemFeatureToggle, UUID> {
    Optional<SystemFeatureToggle> findByToggleKeyAndDeletedFalse(SystemFeatureToggleKey toggleKey);
    java.util.List<SystemFeatureToggle> findAllByDeletedFalseOrderByToggleKeyAsc();
}
