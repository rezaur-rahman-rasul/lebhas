package com.lebhas.creativesaas.monitoring.infrastructure.persistence;

import com.lebhas.creativesaas.monitoring.domain.SystemComponentType;
import com.lebhas.creativesaas.monitoring.domain.SystemHealthEvent;
import com.lebhas.creativesaas.monitoring.domain.SystemHealthStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SystemHealthEventRepository extends JpaRepository<SystemHealthEvent, UUID> {

    Optional<SystemHealthEvent> findBySourceEventIdAndDeletedFalse(String sourceEventId);

    boolean existsBySourceEventIdAndDeletedFalse(String sourceEventId);

    List<SystemHealthEvent> findAllByDeletedFalseOrderByEventAtDesc(Pageable pageable);

    List<SystemHealthEvent> findAllByComponentTypeOrderByEventAtDesc(SystemComponentType componentType);

    List<SystemHealthEvent> findAllByComponentTypeAndDeletedFalseOrderByEventAtDesc(SystemComponentType componentType, Pageable pageable);

    List<SystemHealthEvent> findAllByHealthStatusOrderByEventAtDesc(SystemHealthStatus healthStatus);

    List<SystemHealthEvent> findAllByHealthStatusAndDeletedFalseOrderByEventAtDesc(SystemHealthStatus healthStatus, Pageable pageable);

    List<SystemHealthEvent> findAllByWorkspaceIdOrderByEventAtDesc(UUID workspaceId);

    List<SystemHealthEvent> findAllByWorkspaceIdAndDeletedFalseOrderByEventAtDesc(UUID workspaceId, Pageable pageable);
}
