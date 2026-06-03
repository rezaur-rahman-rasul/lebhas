package com.lebhas.ai.infrastructure.persistence;

import com.lebhas.ai.domain.ProviderHealthSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProviderHealthSnapshotRepository extends JpaRepository<ProviderHealthSnapshot, UUID> {

    Optional<ProviderHealthSnapshot> findFirstByProviderIdAndDeletedFalseOrderByLastCheckedAtDesc(UUID providerId);

    List<ProviderHealthSnapshot> findAllByDeletedFalseOrderByLastCheckedAtDesc();
}
