import { HttpContext } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { Permission, UserRole } from '@app/features/auth/models/user.models';
import {
  CreateWorkspacePayload,
  UpdateWorkspacePayload,
  UpdateWorkspaceSettingsPayload,
  Workspace,
  WorkspaceSettings,
  WorkspaceSummary,
} from '../models/workspace.models';

interface WorkspaceSummaryResponseDto {
  readonly id: string;
  readonly name: string;
  readonly slug: string | null;
  readonly logoUrl: string | null;
  readonly status: Workspace['status'];
  readonly language: Workspace['language'];
  readonly timezone: string;
  readonly ownerId: string;
  readonly currentUserRole: UserRole;
  readonly currentUserPermissions: readonly Permission[];
  readonly createdAt: string;
  readonly updatedAt: string;
}

interface WorkspaceResponseDto extends WorkspaceSummaryResponseDto {
  readonly description: string | null;
  readonly industry: string | null;
  readonly currency: string | null;
  readonly country: string | null;
}

interface WorkspaceSettingsResponseDto {
  readonly workspaceId: string;
  readonly allowCrewDownload: boolean;
  readonly allowCrewPublish: boolean;
  readonly defaultLanguage: Workspace['language'];
  readonly defaultTimezone: string;
  readonly notificationPreferences: WorkspaceSettings['notificationPreferences'];
  readonly workspaceVisibility: WorkspaceSettings['workspaceVisibility'];
  readonly createdAt: string;
  readonly updatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class WorkspaceService {
  private readonly api = inject(ApiService);

  createWorkspace(payload: CreateWorkspacePayload) {
    return this.api
      .post<WorkspaceResponseDto, CreateWorkspacePayload>('/api/v1/workspaces', payload)
      .pipe(map(({ data }) => mapWorkspace(data)));
  }

  getMyWorkspaces(context?: HttpContext) {
    return this.api
      .get<readonly WorkspaceSummaryResponseDto[]>('/api/v1/workspaces/me', { context })
      .pipe(map(({ data }) => data.map(mapWorkspaceSummary)));
  }

  getWorkspace(id: string, context?: HttpContext) {
    return this.api
      .get<WorkspaceResponseDto>(`/api/v1/workspaces/${id}`, { context })
      .pipe(map(({ data }) => mapWorkspace(data)));
  }

  updateWorkspace(id: string, payload: UpdateWorkspacePayload) {
    return this.api
      .put<WorkspaceResponseDto, UpdateWorkspacePayload>(`/api/v1/workspaces/${id}`, payload)
      .pipe(map(({ data }) => mapWorkspace(data)));
  }

  getSettings(id: string, context?: HttpContext) {
    return this.api
      .get<WorkspaceSettingsResponseDto>(`/api/v1/workspaces/${id}/settings`, { context })
      .pipe(map(({ data }) => mapWorkspaceSettings(data)));
  }

  updateSettings(id: string, payload: UpdateWorkspaceSettingsPayload) {
    return this.api
      .put<WorkspaceSettingsResponseDto, UpdateWorkspaceSettingsPayload>(
        `/api/v1/workspaces/${id}/settings`,
        payload,
      )
      .pipe(map(({ data }) => mapWorkspaceSettings(data)));
  }
}

function mapWorkspaceSummary(source: WorkspaceSummaryResponseDto): WorkspaceSummary {
  return {
    id: source.id,
    name: source.name,
    slug: source.slug,
    logoUrl: source.logoUrl,
    status: source.status,
    language: source.language,
    timezone: source.timezone,
    ownerId: source.ownerId,
    currentUserRole: source.currentUserRole,
    currentUserPermissions: source.currentUserPermissions ?? [],
    createdAt: source.createdAt,
    updatedAt: source.updatedAt,
  };
}

function mapWorkspace(source: WorkspaceResponseDto): Workspace {
  return {
    ...mapWorkspaceSummary(source),
    description: source.description,
    industry: source.industry,
    currency: source.currency,
    country: source.country,
  };
}

function mapWorkspaceSettings(source: WorkspaceSettingsResponseDto): WorkspaceSettings {
  return {
    workspaceId: source.workspaceId,
    allowCrewDownload: source.allowCrewDownload,
    allowCrewPublish: source.allowCrewPublish,
    defaultLanguage: source.defaultLanguage,
    defaultTimezone: source.defaultTimezone,
    notificationPreferences: source.notificationPreferences,
    workspaceVisibility: source.workspaceVisibility,
    createdAt: source.createdAt,
    updatedAt: source.updatedAt,
  };
}
