package com.lebhas.creativesaas.monitoring.infrastructure.persistence;

import com.lebhas.creativesaas.monitoring.domain.MonitoringAlert;
import com.lebhas.creativesaas.monitoring.domain.MonitoringAlertStatus;
import com.lebhas.creativesaas.monitoring.domain.MonitoringSeverity;
import com.lebhas.creativesaas.monitoring.domain.SystemComponentType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MonitoringAlertRepository extends JpaRepository<MonitoringAlert, UUID> {

    Optional<MonitoringAlert> findByAlertKeyAndDeletedFalse(String alertKey);

    List<MonitoringAlert> findAllByDeletedFalseOrderByTriggeredAtDesc(Pageable pageable);

    List<MonitoringAlert> findAllByAlertStatusOrderByTriggeredAtDesc(MonitoringAlertStatus alertStatus);

    List<MonitoringAlert> findAllByAlertStatusAndDeletedFalseOrderByTriggeredAtDesc(MonitoringAlertStatus alertStatus, Pageable pageable);

    List<MonitoringAlert> findAllBySeverityOrderByTriggeredAtDesc(MonitoringSeverity severity);

    List<MonitoringAlert> findAllBySeverityAndDeletedFalseOrderByTriggeredAtDesc(MonitoringSeverity severity, Pageable pageable);

    List<MonitoringAlert> findAllByComponentTypeOrderByTriggeredAtDesc(SystemComponentType componentType);

    List<MonitoringAlert> findAllByComponentTypeAndDeletedFalseOrderByTriggeredAtDesc(SystemComponentType componentType, Pageable pageable);

    List<MonitoringAlert> findAllByWorkspaceIdOrderByTriggeredAtDesc(UUID workspaceId);

    List<MonitoringAlert> findAllByWorkspaceIdAndDeletedFalseOrderByTriggeredAtDesc(UUID workspaceId, Pageable pageable);
}
