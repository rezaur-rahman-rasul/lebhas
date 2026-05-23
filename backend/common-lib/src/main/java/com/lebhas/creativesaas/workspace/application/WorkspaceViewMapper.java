package com.lebhas.creativesaas.workspace.application;

import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipEntity;
import com.lebhas.creativesaas.identity.domain.UserEntity;
import com.lebhas.creativesaas.workspace.application.dto.BrandProfileView;
import com.lebhas.creativesaas.workspace.application.dto.CrewMemberView;
import com.lebhas.creativesaas.workspace.application.dto.NotificationPreferencesView;
import com.lebhas.creativesaas.workspace.application.dto.WorkspaceSettingsView;
import com.lebhas.creativesaas.workspace.application.dto.WorkspaceSummaryView;
import com.lebhas.creativesaas.workspace.application.dto.WorkspaceView;
import com.lebhas.creativesaas.workspace.domain.BrandProfileEntity;
import com.lebhas.creativesaas.workspace.domain.WorkspaceEntity;
import com.lebhas.creativesaas.workspace.domain.WorkspaceNotificationPreferences;
import com.lebhas.creativesaas.workspace.domain.WorkspaceSettingsEntity;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class WorkspaceViewMapper {

    public WorkspaceSummaryView toWorkspaceSummaryView(WorkspaceEntity workspace, Role currentUserRole, Set<Permission> permissions) {
        return new WorkspaceSummaryView(
                workspace.getId(),
                workspace.getName(),
                workspace.getSlug(),
                workspace.getLogoUrl(),
                workspace.getStatus(),
                workspace.getLanguage(),
                workspace.getTimezone(),
                workspace.getOwnerId(),
                currentUserRole,
                Set.copyOf(permissions),
                workspace.getCreatedAt(),
                workspace.getUpdatedAt());
    }

    public WorkspaceView toWorkspaceView(WorkspaceEntity workspace, Role currentUserRole, Set<Permission> permissions) {
        return new WorkspaceView(
                workspace.getId(),
                workspace.getName(),
                workspace.getSlug(),
                workspace.getLogoUrl(),
                workspace.getDescription(),
                workspace.getIndustry(),
                workspace.getTimezone(),
                workspace.getLanguage(),
                workspace.getCurrency(),
                workspace.getCountry(),
                workspace.getStatus(),
                workspace.getOwnerId(),
                currentUserRole,
                Set.copyOf(permissions),
                workspace.getCreatedAt(),
                workspace.getUpdatedAt());
    }

    public BrandProfileView toBrandProfileView(BrandProfileEntity profile) {
        return new BrandProfileView(
                profile.getId(),
                profile.getWorkspaceId(),
                profile.getBrandName(),
                profile.getBusinessType(),
                profile.getIndustry(),
                profile.getTargetAudience(),
                profile.getBrandVoice(),
                profile.getPreferredCta(),
                profile.getPrimaryColor(),
                profile.getSecondaryColor(),
                profile.getWebsite(),
                profile.getFacebookUrl(),
                profile.getInstagramUrl(),
                profile.getLinkedinUrl(),
                profile.getTiktokUrl(),
                profile.getDescription(),
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }

    public WorkspaceSettingsView toWorkspaceSettingsView(WorkspaceSettingsEntity settings) {
        return new WorkspaceSettingsView(
                settings.getWorkspaceId(),
                settings.isAllowCrewDownload(),
                settings.isAllowCrewPublish(),
                settings.getDefaultLanguage(),
                settings.getDefaultTimezone(),
                toNotificationPreferencesView(settings.getNotificationPreferences()),
                settings.getWorkspaceVisibility(),
                settings.getCreatedAt(),
                settings.getUpdatedAt());
    }

    public CrewMemberView toCrewMemberView(UserEntity user, WorkspaceMembershipEntity membership, Set<Permission> permissions) {
        return new CrewMemberView(
                user.getId(),
                membership.getWorkspaceId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                membership.getRole(),
                membership.getStatus(),
                Set.copyOf(permissions),
                membership.getJoinedAt(),
                membership.getInvitedByUserId(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    public NotificationPreferencesView toNotificationPreferencesView(WorkspaceNotificationPreferences preferences) {
        return new NotificationPreferencesView(
                preferences.isCrewInvites(),
                preferences.isWorkspaceUpdates(),
                preferences.isSecurityAlerts());
    }
}
