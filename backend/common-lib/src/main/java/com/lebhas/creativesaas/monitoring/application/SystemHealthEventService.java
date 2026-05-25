package com.lebhas.creativesaas.monitoring.application;

import com.lebhas.creativesaas.monitoring.cache.MonitoringAlertCacheService;
import com.lebhas.creativesaas.monitoring.domain.MonitoringSeverity;
import com.lebhas.creativesaas.monitoring.domain.SystemComponentType;
import com.lebhas.creativesaas.monitoring.domain.SystemHealthEvent;
import com.lebhas.creativesaas.monitoring.domain.SystemHealthStatus;
import com.lebhas.creativesaas.monitoring.infrastructure.persistence.SystemHealthEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class SystemHealthEventService {

    private final SystemHealthEventRepository systemHealthEventRepository;
    private final MonitoringMapper monitoringMapper;
    private MonitoringAlertCacheService monitoringAlertCacheService;

    public SystemHealthEventService(
            SystemHealthEventRepository systemHealthEventRepository,
            MonitoringMapper monitoringMapper
    ) {
        this.systemHealthEventRepository = systemHealthEventRepository;
        this.monitoringMapper = monitoringMapper;
    }

    @Autowired(required = false)
    void setMonitoringAlertCacheService(MonitoringAlertCacheService monitoringAlertCacheService) {
        this.monitoringAlertCacheService = monitoringAlertCacheService;
    }

    @Transactional
    public Optional<SystemHealthEventView> record(SystemHealthEventCommand command) {
        if (systemHealthEventRepository.existsBySourceEventIdAndDeletedFalse(command.sourceEventId())) {
            return Optional.empty();
        }
        SystemHealthEvent event = monitoringMapper.toEntity(command);
        SystemHealthEvent saved = systemHealthEventRepository.save(event);
        invalidateSystemHealthCache();
        return Optional.of(monitoringMapper.toView(saved));
    }

    @Transactional
    public Optional<SystemHealthEventView> record(
            UUID workspaceId,
            String sourceEventId,
            SystemComponentType componentType,
            String componentName,
            SystemHealthStatus healthStatus,
            MonitoringSeverity severity,
            String message,
            Map<String, ?> details
    ) {
        return record(new SystemHealthEventCommand(
                workspaceId,
                sourceEventId,
                componentType,
                componentName,
                healthStatus,
                severity,
                message,
                monitoringMapper.detailsJson(details),
                Instant.now()));
    }

    @Transactional
    public Optional<SystemHealthEventView> aiProviderFailure(String sourceEventId, String providerCode, String message, Map<String, ?> details) {
        return record(null, sourceEventId, SystemComponentType.AI, providerCode, SystemHealthStatus.DEGRADED, MonitoringSeverity.ERROR, message, details);
    }

    @Transactional
    public Optional<SystemHealthEventView> aiProviderRecovery(String sourceEventId, String providerCode, String message, Map<String, ?> details) {
        return record(null, sourceEventId, SystemComponentType.AI, providerCode, SystemHealthStatus.RECOVERED, MonitoringSeverity.INFO, message, details);
    }

    @Transactional
    public Optional<SystemHealthEventView> paymentFailure(UUID workspaceId, String sourceEventId, String message, Map<String, ?> details) {
        return record(workspaceId, sourceEventId, SystemComponentType.PAYMENT, "payment", SystemHealthStatus.DEGRADED, MonitoringSeverity.ERROR, message, details);
    }

    @Transactional
    public Optional<SystemHealthEventView> webhookFailure(UUID workspaceId, String sourceEventId, String providerCode, String message, Map<String, ?> details) {
        return record(workspaceId, sourceEventId, SystemComponentType.PAYMENT, "webhook:" + providerCode, SystemHealthStatus.DEGRADED, MonitoringSeverity.ERROR, message, details);
    }

    @Transactional
    public Optional<SystemHealthEventView> kafkaFailure(String sourceEventId, String topic, String message, Map<String, ?> details) {
        return record(null, sourceEventId, SystemComponentType.KAFKA, topic, SystemHealthStatus.DEGRADED, MonitoringSeverity.ERROR, message, details);
    }

    @Transactional
    public Optional<SystemHealthEventView> redisFailure(String sourceEventId, String operation, String message, Map<String, ?> details) {
        return record(null, sourceEventId, SystemComponentType.REDIS, operation, SystemHealthStatus.DEGRADED, MonitoringSeverity.ERROR, message, details);
    }

    @Transactional
    public Optional<SystemHealthEventView> storageLimitExceeded(UUID workspaceId, String sourceEventId, String message, Map<String, ?> details) {
        return record(workspaceId, sourceEventId, SystemComponentType.STORAGE, "storage-limit", SystemHealthStatus.DEGRADED, MonitoringSeverity.WARNING, message, details);
    }

    @Transactional
    public Optional<SystemHealthEventView> creditLow(UUID workspaceId, String sourceEventId, String message, Map<String, ?> details) {
        return record(workspaceId, sourceEventId, SystemComponentType.USAGE, "credit-balance", SystemHealthStatus.DEGRADED, MonitoringSeverity.WARNING, message, details);
    }

    @Transactional
    public Optional<SystemHealthEventView> generationFailure(UUID workspaceId, String sourceEventId, String message, Map<String, ?> details) {
        return record(workspaceId, sourceEventId, SystemComponentType.CREATIVE, "generation", SystemHealthStatus.DEGRADED, MonitoringSeverity.ERROR, message, details);
    }

    @Transactional
    public Optional<SystemHealthEventView> systemAlert(String sourceEventId, String componentName, MonitoringSeverity severity, String message, Map<String, ?> details) {
        return record(null, sourceEventId, SystemComponentType.APPLICATION, componentName, SystemHealthStatus.DEGRADED, severity, message, details);
    }

    private void invalidateSystemHealthCache() {
        if (monitoringAlertCacheService != null) {
            monitoringAlertCacheService.invalidateSystemHealth();
        }
    }
}
