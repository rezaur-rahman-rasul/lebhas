package com.lebhas.creativesaas.auditlog.infrastructure.persistence;

import com.lebhas.creativesaas.auditlog.domain.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Optional<AuditLog> findBySourceEventIdAndDeletedFalse(String sourceEventId);

    boolean existsBySourceEventIdAndDeletedFalse(String sourceEventId);

    List<AuditLog> findAllByWorkspaceIdAndDeletedFalseOrderByAuditAtDesc(UUID workspaceId);

    List<AuditLog> findAllByWorkspaceIdAndDeletedFalseOrderByAuditAtDesc(UUID workspaceId, Pageable pageable);

    List<AuditLog> findAllByDeletedFalseOrderByAuditAtDesc(Pageable pageable);

    List<AuditLog> findAllByWorkspaceIdAndActorUserIdAndDeletedFalseOrderByAuditAtDesc(
            UUID workspaceId,
            UUID actorUserId,
            Pageable pageable);

    List<AuditLog> findAllByWorkspaceIdAndEntityTypeAndEntityIdAndDeletedFalseOrderByAuditAtDesc(
            UUID workspaceId,
            String entityType,
            UUID entityId);

    List<AuditLog> findAllByWorkspaceIdAndEntityTypeAndEntityIdAndDeletedFalseOrderByAuditAtDesc(
            UUID workspaceId,
            String entityType,
            UUID entityId,
            Pageable pageable);
}
