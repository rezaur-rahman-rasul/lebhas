package com.lebhas.creativesaas.auditlog.application;

import com.lebhas.creativesaas.auditlog.domain.AuditActionType;
import com.lebhas.creativesaas.auditlog.domain.AuditOutcome;

import java.time.Instant;
import java.util.UUID;

public record AuditLogView(
        UUID id,
        UUID workspaceId,
        String sourceEventId,
        UUID actorUserId,
        AuditActionType actionType,
        AuditOutcome outcome,
        String entityType,
        UUID entityId,
        String summary,
        String metadataJson,
        String ipAddress,
        String userAgent,
        Instant auditAt,
        Instant createdAt
) {
}
