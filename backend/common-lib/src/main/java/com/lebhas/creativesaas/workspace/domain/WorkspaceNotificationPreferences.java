package com.lebhas.creativesaas.workspace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class WorkspaceNotificationPreferences {

    @Column(name = "notify_crew_invites", nullable = false)
    private boolean crewInvites;

    @Column(name = "notify_workspace_updates", nullable = false)
    private boolean workspaceUpdates;

    @Column(name = "notify_security_alerts", nullable = false)
    private boolean securityAlerts;

    protected WorkspaceNotificationPreferences() {
    }

    private WorkspaceNotificationPreferences(boolean crewInvites, boolean workspaceUpdates, boolean securityAlerts) {
        this.crewInvites = crewInvites;
        this.workspaceUpdates = workspaceUpdates;
        this.securityAlerts = securityAlerts;
    }

    public static WorkspaceNotificationPreferences defaults() {
        return new WorkspaceNotificationPreferences(true, true, true);
    }

    public static WorkspaceNotificationPreferences of(boolean crewInvites, boolean workspaceUpdates, boolean securityAlerts) {
        return new WorkspaceNotificationPreferences(crewInvites, workspaceUpdates, securityAlerts);
    }

    public boolean isCrewInvites() {
        return crewInvites;
    }

    public boolean isWorkspaceUpdates() {
        return workspaceUpdates;
    }

    public boolean isSecurityAlerts() {
        return securityAlerts;
    }
}
