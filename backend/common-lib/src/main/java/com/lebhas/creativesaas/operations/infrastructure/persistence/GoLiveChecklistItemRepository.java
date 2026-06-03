package com.lebhas.creativesaas.operations.infrastructure.persistence;

import com.lebhas.creativesaas.operations.domain.GoLiveChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoLiveChecklistItemRepository extends JpaRepository<GoLiveChecklistItem, UUID> {
    List<GoLiveChecklistItem> findAllByDeletedFalseOrderByCreatedAtAsc();
    Optional<GoLiveChecklistItem> findByIdAndDeletedFalse(UUID id);
}
