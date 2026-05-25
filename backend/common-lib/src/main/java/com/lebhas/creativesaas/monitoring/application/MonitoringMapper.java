package com.lebhas.creativesaas.monitoring.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.monitoring.domain.MonitoringAlert;
import com.lebhas.creativesaas.monitoring.domain.SystemHealthEvent;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MonitoringMapper {

    private final ObjectMapper objectMapper;

    public MonitoringMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SystemHealthEvent toEntity(SystemHealthEventCommand command) {
        return SystemHealthEvent.create(
                command.workspaceId(),
                command.sourceEventId(),
                command.componentType(),
                command.componentName(),
                command.healthStatus(),
                command.severity(),
                command.message(),
                command.detailsJson(),
                command.eventAt());
    }

    public SystemHealthEventView toView(SystemHealthEvent event) {
        return new SystemHealthEventView(
                event.getId(),
                event.getWorkspaceId(),
                event.getSourceEventId(),
                event.getComponentType(),
                event.getComponentName(),
                event.getHealthStatus(),
                event.getSeverity(),
                event.getMessage(),
                event.getDetailsJson(),
                event.getEventAt(),
                event.getCreatedAt());
    }

    public MonitoringAlert toEntity(MonitoringAlertCommand command) {
        return MonitoringAlert.create(
                command.workspaceId(),
                command.alertKey(),
                command.componentType(),
                command.componentName(),
                command.severity(),
                command.title(),
                command.description(),
                command.triggeredAt());
    }

    public MonitoringAlertView toView(MonitoringAlert alert) {
        return new MonitoringAlertView(
                alert.getId(),
                alert.getWorkspaceId(),
                alert.getAlertKey(),
                alert.getComponentType(),
                alert.getComponentName(),
                alert.getSeverity(),
                alert.getAlertStatus(),
                alert.getTitle(),
                alert.getDescription(),
                alert.getTriggeredAt(),
                alert.getAcknowledgedAt(),
                alert.getResolvedAt(),
                alert.getCreatedAt());
    }

    public String detailsJson(Map<String, ?> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Monitoring details must be JSON serializable");
        }
    }
}
