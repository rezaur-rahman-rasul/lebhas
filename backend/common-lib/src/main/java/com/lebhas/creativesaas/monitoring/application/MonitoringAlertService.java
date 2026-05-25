package com.lebhas.creativesaas.monitoring.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.monitoring.cache.MonitoringAlertCacheService;
import com.lebhas.creativesaas.monitoring.domain.MonitoringAlert;
import com.lebhas.creativesaas.monitoring.infrastructure.persistence.MonitoringAlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class MonitoringAlertService {

    private final MonitoringAlertRepository monitoringAlertRepository;
    private final MonitoringMapper monitoringMapper;
    private MonitoringAlertCacheService monitoringAlertCacheService;

    public MonitoringAlertService(
            MonitoringAlertRepository monitoringAlertRepository,
            MonitoringMapper monitoringMapper
    ) {
        this.monitoringAlertRepository = monitoringAlertRepository;
        this.monitoringMapper = monitoringMapper;
    }

    @Autowired(required = false)
    void setMonitoringAlertCacheService(MonitoringAlertCacheService monitoringAlertCacheService) {
        this.monitoringAlertCacheService = monitoringAlertCacheService;
    }

    @Transactional
    public MonitoringAlertView openAlert(MonitoringAlertCommand command) {
        Optional<MonitoringAlert> existing = monitoringAlertRepository.findByAlertKeyAndDeletedFalse(command.alertKey());
        if (existing.isPresent()) {
            return monitoringMapper.toView(existing.get());
        }
        MonitoringAlert alert = monitoringMapper.toEntity(command);
        MonitoringAlert saved = monitoringAlertRepository.save(alert);
        invalidateAlertCache();
        return monitoringMapper.toView(saved);
    }

    @Transactional
    public MonitoringAlertView acknowledge(UUID alertId) {
        MonitoringAlert alert = monitoringAlertRepository.findById(alertId)
                .filter(existing -> !existing.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Monitoring alert not found"));
        alert.acknowledge(Instant.now());
        MonitoringAlert saved = monitoringAlertRepository.save(alert);
        invalidateAlertCache();
        return monitoringMapper.toView(saved);
    }

    @Transactional
    public MonitoringAlertView resolve(UUID alertId) {
        MonitoringAlert alert = monitoringAlertRepository.findById(alertId)
                .filter(existing -> !existing.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Monitoring alert not found"));
        alert.resolve(Instant.now());
        MonitoringAlert saved = monitoringAlertRepository.save(alert);
        invalidateAlertCache();
        return monitoringMapper.toView(saved);
    }

    private void invalidateAlertCache() {
        if (monitoringAlertCacheService != null) {
            monitoringAlertCacheService.invalidateActiveAlerts();
        }
    }
}
