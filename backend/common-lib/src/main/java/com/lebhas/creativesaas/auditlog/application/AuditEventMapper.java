package com.lebhas.creativesaas.auditlog.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.creativesaas.auditlog.domain.AuditLog;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AuditEventMapper {

    private final ObjectMapper objectMapper;

    public AuditEventMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AuditLog toEntity(AuditLogCommand command) {
        return AuditLog.create(
                command.workspaceId(),
                command.sourceEventId(),
                command.actorUserId(),
                command.actionType(),
                command.outcome(),
                command.entityType(),
                command.entityId(),
                command.summary(),
                command.metadataJson(),
                command.ipAddress(),
                command.userAgent(),
                command.auditAt());
    }

    public AuditLogView toView(AuditLog auditLog) {
        return new AuditLogView(
                auditLog.getId(),
                auditLog.getWorkspaceId(),
                auditLog.getSourceEventId(),
                auditLog.getActorUserId(),
                auditLog.getActionType(),
                auditLog.getOutcome(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getSummary(),
                auditLog.getMetadataJson(),
                auditLog.getIpAddress(),
                auditLog.getUserAgent(),
                auditLog.getAuditAt(),
                auditLog.getCreatedAt());
    }

    public String metadataJson(Map<String, ?> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Audit metadata must be JSON serializable");
        }
    }
}
