package com.lebhas.creativesaas.monitoring.application;

import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.monitoring.infrastructure.persistence.MonitoringAlertRepository;
import com.lebhas.creativesaas.monitoring.infrastructure.persistence.SystemHealthEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OperationalMonitoringService {

    private final SystemHealthEventRepository systemHealthEventRepository;
    private final MonitoringAlertRepository monitoringAlertRepository;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final MonitoringMapper monitoringMapper;

    public OperationalMonitoringService(
            SystemHealthEventRepository systemHealthEventRepository,
            MonitoringAlertRepository monitoringAlertRepository,
            WorkspaceAuthorizationService workspaceAuthorizationService,
            MonitoringMapper monitoringMapper
    ) {
        this.systemHealthEventRepository = systemHealthEventRepository;
        this.monitoringAlertRepository = monitoringAlertRepository;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.monitoringMapper = monitoringMapper;
    }

    @Transactional(readOnly = true)
    public List<SystemHealthEventView> workspaceHealthEvents(UUID workspaceId, int limit) {
        workspaceAuthorizationService.requirePermission(workspaceId, Permission.WORKSPACE_VIEW);
        return systemHealthEventRepository.findAllByWorkspaceIdAndDeletedFalseOrderByEventAtDesc(
                        workspaceId,
                        PageRequest.of(0, normalizeLimit(limit)))
                .stream()
                .map(monitoringMapper::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MonitoringAlertView> workspaceAlerts(UUID workspaceId, int limit) {
        workspaceAuthorizationService.requirePermission(workspaceId, Permission.WORKSPACE_VIEW);
        return monitoringAlertRepository.findAllByWorkspaceIdAndDeletedFalseOrderByTriggeredAtDesc(
                        workspaceId,
                        PageRequest.of(0, normalizeLimit(limit)))
                .stream()
                .map(monitoringMapper::toView)
                .toList();
    }

    private static int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 50;
        }
        return Math.min(limit, 200);
    }
}
