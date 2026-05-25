package com.lebhas.creativesaas.monitoring.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.monitoring.domain.MonitoringAlertStatus;
import com.lebhas.creativesaas.monitoring.domain.SystemComponentType;
import com.lebhas.creativesaas.monitoring.infrastructure.persistence.MonitoringAlertRepository;
import com.lebhas.creativesaas.monitoring.infrastructure.persistence.SystemHealthEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MasterMonitoringService {

    private final SystemHealthEventRepository systemHealthEventRepository;
    private final MonitoringAlertRepository monitoringAlertRepository;
    private final MonitoringAlertService monitoringAlertService;
    private final MonitoringMapper monitoringMapper;
    private final CurrentUserContext currentUserContext;

    public MasterMonitoringService(
            SystemHealthEventRepository systemHealthEventRepository,
            MonitoringAlertRepository monitoringAlertRepository,
            MonitoringAlertService monitoringAlertService,
            MonitoringMapper monitoringMapper,
            CurrentUserContext currentUserContext
    ) {
        this.systemHealthEventRepository = systemHealthEventRepository;
        this.monitoringAlertRepository = monitoringAlertRepository;
        this.monitoringAlertService = monitoringAlertService;
        this.monitoringMapper = monitoringMapper;
        this.currentUserContext = currentUserContext;
    }

    @Transactional(readOnly = true)
    public List<SystemHealthEventView> recentHealthEvents(int limit) {
        requireMaster();
        return systemHealthEventRepository.findAllByDeletedFalseOrderByEventAtDesc(PageRequest.of(0, normalizeLimit(limit)))
                .stream()
                .map(monitoringMapper::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SystemHealthEventView> healthEventsByComponent(SystemComponentType componentType, int limit) {
        requireMaster();
        return systemHealthEventRepository.findAllByComponentTypeAndDeletedFalseOrderByEventAtDesc(
                        componentType,
                        PageRequest.of(0, normalizeLimit(limit)))
                .stream()
                .map(monitoringMapper::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MonitoringAlertView> recentAlerts(int limit) {
        requireMaster();
        return monitoringAlertRepository.findAllByDeletedFalseOrderByTriggeredAtDesc(PageRequest.of(0, normalizeLimit(limit)))
                .stream()
                .map(monitoringMapper::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MonitoringAlertView> alertsByStatus(MonitoringAlertStatus status, int limit) {
        requireMaster();
        return monitoringAlertRepository.findAllByAlertStatusAndDeletedFalseOrderByTriggeredAtDesc(
                        status,
                        PageRequest.of(0, normalizeLimit(limit)))
                .stream()
                .map(monitoringMapper::toView)
                .toList();
    }

    @Transactional
    public MonitoringAlertView acknowledgeAlert(UUID alertId) {
        requireMaster();
        return monitoringAlertService.acknowledge(alertId);
    }

    @Transactional
    public MonitoringAlertView resolveAlert(UUID alertId) {
        requireMaster();
        return monitoringAlertService.resolve(alertId);
    }

    private void requireMaster() {
        if (!currentUserContext.requireCurrentUser().isMaster()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "MASTER access required");
        }
    }

    private static int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 50;
        }
        return Math.min(limit, 200);
    }
}
