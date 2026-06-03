package com.lebhas.creativesaas.operations.infrastructure.persistence;

import com.lebhas.creativesaas.operations.domain.SmokeTestRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SmokeTestRunRepository extends JpaRepository<SmokeTestRun, UUID> {
    List<SmokeTestRun> findAllByDeletedFalseOrderByStartedAtDesc();
    Optional<SmokeTestRun> findByIdAndDeletedFalse(UUID id);
}
