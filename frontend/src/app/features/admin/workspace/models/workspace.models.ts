import { Permission, UserRole } from '@app/features/auth/models/user.models';

export type WorkspaceLanguage = 'ENGLISH' | 'BANGLA';
export type WorkspaceStatus = 'ACTIVE' | 'SUSPENDED' | 'ARCHIVED';
export type WorkspaceVisibility = 'PRIVATE' | 'INTERNAL' | 'PUBLIC';

export interface WorkspaceSummary {
  readonly id: string;
  readonly name: string;
  readonly slug: string | null;
  readonly logoUrl: string | null;
  readonly status: WorkspaceStatus;
  readonly language: WorkspaceLanguage;
  readonly timezone: string;
  readonly ownerId: string;
  readonly currentUserRole: UserRole;
  readonly currentUserPermissions: readonly Permission[];
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface Workspace extends WorkspaceSummary {
  readonly description: string | null;
  readonly industry: string | null;
  readonly currency: string | null;
  readonly country: string | null;
}

export interface WorkspaceNotificationPreferences {
  readonly crewInvites: boolean;
  readonly workspaceUpdates: boolean;
  readonly securityAlerts: boolean;
}

export interface WorkspaceSettings {
  readonly workspaceId: string;
  readonly allowCrewDownload: boolean;
  readonly allowCrewPublish: boolean;
  readonly defaultLanguage: WorkspaceLanguage;
  readonly defaultTimezone: string;
  readonly notificationPreferences: WorkspaceNotificationPreferences;
  readonly workspaceVisibility: WorkspaceVisibility;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface CreateWorkspacePayload {
  readonly name: string;
  readonly slug: string | null;
  readonly logoUrl: string | null;
  readonly description: string | null;
  readonly industry: string | null;
  readonly timezone: string;
  readonly language: WorkspaceLanguage;
  readonly currency: string;
  readonly country: string;
}

export interface UpdateWorkspacePayload extends CreateWorkspacePayload {
  readonly status: WorkspaceStatus;
}

export interface UpdateWorkspaceSettingsPayload {
  readonly allowCrewDownload: boolean;
  readonly allowCrewPublish: boolean;
  readonly defaultLanguage: WorkspaceLanguage;
  readonly defaultTimezone: string;
  readonly notificationPreferences: WorkspaceNotificationPreferences;
  readonly workspaceVisibility: WorkspaceVisibility;
}

export interface WorkspaceSettingsFormValue {
  readonly name: string;
  readonly description: string;
  readonly industry: string;
  readonly timezone: string;
  readonly language: WorkspaceLanguage;
  readonly currency: string;
  readonly country: string;
  readonly logoUrl: string;
  readonly allowCrewDownload: boolean;
  readonly allowCrewPublish: boolean;
  readonly workspaceVisibility: WorkspaceVisibility;
}

export interface SaveWorkspaceSettingsPayload {
  readonly workspace: UpdateWorkspacePayload;
  readonly settings: UpdateWorkspaceSettingsPayload;
}

export const WORKSPACE_LANGUAGE_OPTIONS: readonly {
  readonly value: WorkspaceLanguage;
  readonly label: string;
}[] = [
  { value: 'ENGLISH', label: 'English' },
  { value: 'BANGLA', label: 'Bangla' },
];

export const WORKSPACE_VISIBILITY_OPTIONS: readonly {
  readonly value: WorkspaceVisibility;
  readonly label: string;
  readonly description: string;
}[] = [
  {
    value: 'PRIVATE',
    label: 'Private',
    description: 'Only assigned members can view workspace resources.',
  },
  {
    value: 'INTERNAL',
    label: 'Internal',
    description: 'Visible to your internal team once access is granted.',
  },
  {
    value: 'PUBLIC',
    label: 'Public',
    description: 'Reserved for later sharing workflows across teams.',
  },
];

export const WORKSPACE_STATUS_LABELS: Record<WorkspaceStatus, string> = {
  ACTIVE: 'Active',
  SUSPENDED: 'Suspended',
  ARCHIVED: 'Archived',
};

export const WORKSPACE_LANGUAGE_LABELS: Record<WorkspaceLanguage, string> = {
  ENGLISH: 'English',
  BANGLA: 'Bangla',
};

export const WORKSPACE_VISIBILITY_LABELS: Record<WorkspaceVisibility, string> = {
  PRIVATE: 'Private',
  INTERNAL: 'Internal',
  PUBLIC: 'Public',
};

export const DEFAULT_WORKSPACE_NOTIFICATION_PREFERENCES: WorkspaceNotificationPreferences = {
  crewInvites: true,
  workspaceUpdates: true,
  securityAlerts: true,
};
