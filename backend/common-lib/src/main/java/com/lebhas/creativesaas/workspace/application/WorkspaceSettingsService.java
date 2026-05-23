package com.lebhas.creativesaas.workspace.application;

import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.workspace.application.dto.NotificationPreferencesView;
import com.lebhas.creativesaas.workspace.application.dto.UpdateWorkspaceSettingsCommand;
import com.lebhas.creativesaas.workspace.application.dto.WorkspaceSettingsView;
import com.lebhas.creativesaas.workspace.domain.WorkspaceNotificationPreferences;
import com.lebhas.creativesaas.workspace.domain.WorkspaceSettingsEntity;
import com.lebhas.creativesaas.workspace.infrastructure.persistence.WorkspaceSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceSettingsService {

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final WorkspaceProvisioningService workspaceProvisioningService;
    private final WorkspaceSettingsRepository workspaceSettingsRepository;
    private final WorkspaceViewMapper workspaceViewMapper;
    private final WorkspaceActivityLogger workspaceActivityLogger;

    public WorkspaceSettingsService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            WorkspaceProvisioningService workspaceProvisioningService,
            WorkspaceSettingsRepository workspaceSettingsRepository,
            WorkspaceViewMapper workspaceViewMapper,
            WorkspaceActivityLogger workspaceActivityLogger
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.workspaceProvisioningService = workspaceProvisioningService;
        this.workspaceSettingsRepository = workspaceSettingsRepository;
        this.workspaceViewMapper = workspaceViewMapper;
        this.workspaceActivityLogger = workspaceActivityLogger;
    }

    @Transactional
    public WorkspaceSettingsView getSettings(java.util.UUID workspaceId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService.requireWorkspaceOwnerOrMaster(workspaceId);
        WorkspaceSettingsEntity settings = workspaceSettingsRepository.findById(access.workspace().getId())
                .orElseGet(() -> workspaceProvisioningService.ensureSettings(access.workspace()));
        return workspaceViewMapper.toWorkspaceSettingsView(settings);
    }

    @Transactional
    public WorkspaceSettingsView updateSettings(UpdateWorkspaceSettingsCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService.requireWorkspaceOwnerOrMaster(command.workspaceId());
        WorkspaceSettingsEntity settings = workspaceSettingsRepository.findById(access.workspace().getId())
                .orElseGet(() -> workspaceProvisioningService.ensureSettings(access.workspace()));
        settings.update(
                command.allowCrewDownload(),
                command.allowCrewPublish(),
                command.defaultLanguage(),
                command.defaultTimezone(),
                toNotificationPreferences(command.notificationPreferences()),
                command.workspaceVisibility());
        workspaceSettingsRepository.save(settings);
        workspaceActivityLogger.logSettingsUpdated(access.workspace().getId(), access.currentUser().userId());
        return workspaceViewMapper.toWorkspaceSettingsView(settings);
    }

    private WorkspaceNotificationPreferences toNotificationPreferences(NotificationPreferencesView view) {
        return WorkspaceNotificationPreferences.of(view.crewInvites(), view.workspaceUpdates(), view.securityAlerts());
    }
}
