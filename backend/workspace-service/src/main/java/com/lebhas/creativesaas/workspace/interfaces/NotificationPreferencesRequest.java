package com.lebhas.creativesaas.workspace.interfaces;

public record NotificationPreferencesRequest(
        boolean crewInvites,
        boolean workspaceUpdates,
        boolean securityAlerts
) {
}
