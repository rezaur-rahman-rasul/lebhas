package com.lebhas.creativesaas.notification.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.monitoring.application.MasterMonitoringService;
import com.lebhas.creativesaas.monitoring.application.MonitoringAlertView;
import com.lebhas.creativesaas.monitoring.application.SystemHealthEventView;
import com.lebhas.creativesaas.monitoring.domain.MonitoringAlertStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/master/monitoring")
@Tag(name = "Master Monitoring")
@SecurityRequirement(name = "bearerAuth")
public class MasterMonitoringController {

    private final MasterMonitoringService masterMonitoringService;

    public MasterMonitoringController(MasterMonitoringService masterMonitoringService) {
        this.masterMonitoringService = masterMonitoringService;
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "List monitoring alerts")
    public ApiResponse<List<MonitoringAlertView>> alerts(
            @RequestParam(required = false) MonitoringAlertStatus status,
            @RequestParam(defaultValue = "50") int limit
    ) {
        if (status != null) {
            return ApiResponse.success(masterMonitoringService.alertsByStatus(status, limit));
        }
        return ApiResponse.success(masterMonitoringService.recentAlerts(limit));
    }

    @GetMapping("/health")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "List system health events")
    public ApiResponse<List<SystemHealthEventView>> health(@RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.success(masterMonitoringService.recentHealthEvents(limit));
    }

    @PostMapping("/alerts/{alertId}/resolve")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Resolve a monitoring alert")
    public ApiResponse<MonitoringAlertView> resolveAlert(@PathVariable UUID alertId) {
        return ApiResponse.success(masterMonitoringService.resolveAlert(alertId));
    }
}
