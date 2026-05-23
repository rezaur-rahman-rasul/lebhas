package com.lebhas.creativesaas.workspace.application.dto;

public record NotificationPreferencesView(
        boolean crewInvites,
        boolean workspaceUpdates,
        boolean securityAlerts
) {
}
