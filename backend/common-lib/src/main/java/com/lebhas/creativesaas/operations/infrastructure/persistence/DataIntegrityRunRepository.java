package com.lebhas.creativesaas.operations.infrastructure.persistence;

import com.lebhas.creativesaas.operations.domain.DataIntegrityRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DataIntegrityRunRepository extends JpaRepository<DataIntegrityRun, UUID> {
    List<DataIntegrityRun> findAllByDeletedFalseOrderByStartedAtDesc();
    Optional<DataIntegrityRun> findByIdAndDeletedFalse(UUID id);
}
