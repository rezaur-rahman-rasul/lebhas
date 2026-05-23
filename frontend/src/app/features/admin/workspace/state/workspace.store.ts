import { HttpContext } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { normalizeHttpError } from '@app/core/api/http-error';
import { SKIP_ERROR_TOAST } from '@app/core/auth/auth-request-context';
import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import { ApiError } from '@app/shared/models/api-response.model';
import { BrandProfile, UpdateBrandProfilePayload } from '../models/brand-profile.models';
import {
  CrewMember,
  InviteCrewPayload,
  UpdateCrewMemberPayload,
  normalizeCrewPermissions,
} from '../models/crew.models';
import {
  DEFAULT_WORKSPACE_NOTIFICATION_PREFERENCES,
  SaveWorkspaceSettingsPayload,
  Workspace,
  WorkspaceSettings,
  WorkspaceSummary,
} from '../models/workspace.models';
import { BrandProfileService } from '../services/brand-profile.service';
import { CrewService } from '../services/crew.service';
import { WorkspaceService } from '../services/workspace.service';

export interface WorkspaceActionResult {
  readonly ok: boolean;
  readonly message?: string;
  readonly fieldErrors: Readonly<Record<string, string>>;
}

@Injectable({ providedIn: 'root' })
export class WorkspaceStore {
  private readonly auth = inject(CurrentUserStore);
  private readonly notifications = inject(NotificationStateService);
  private readonly workspaceService = inject(WorkspaceService);
  private readonly brandProfileService = inject(BrandProfileService);
  private readonly crewService = inject(CrewService);

  private readonly accessibleWorkspacesSignal = signal<readonly WorkspaceSummary[]>([]);
  private readonly currentWorkspaceSignal = signal<Workspace | null>(null);
  private readonly workspaceSettingsSignal = signal<WorkspaceSettings | null>(null);
  private readonly brandProfileSignal = signal<BrandProfile | null>(null);
  private readonly crewMembersSignal = signal<readonly CrewMember[]>([]);
  private readonly workspaceLoadingSignal = signal(false);
  private readonly workspaceErrorSignal = signal<string | null>(null);

  readonly accessibleWorkspaces = this.accessibleWorkspacesSignal.asReadonly();
  readonly currentWorkspace = this.currentWorkspaceSignal.asReadonly();
  readonly workspaceSettings = this.workspaceSettingsSignal.asReadonly();
  readonly brandProfile = this.brandProfileSignal.asReadonly();
  readonly crewMembers = this.crewMembersSignal.asReadonly();
  readonly workspaceLoading = this.workspaceLoadingSignal.asReadonly();
  readonly workspaceError = this.workspaceErrorSignal.asReadonly();

  readonly hasWorkspace = computed(
    () => Boolean(this.currentWorkspaceSignal() ?? this.auth.activeWorkspaceId()),
  );
  readonly selectedWorkspaceSummary = computed(() => {
    const activeWorkspaceId = this.auth.activeWorkspaceId();
    if (!activeWorkspaceId) {
      return null;
    }

    return (
      this.accessibleWorkspacesSignal().find((workspace) => workspace.id === activeWorkspaceId) ??
      null
    );
  });
  readonly brandProfileCompletion = computed(() => {
    const profile = this.brandProfileSignal();
    if (!profile) {
      return 0;
    }

    const completedFields = [
      profile.brandName,
      profile.businessType,
      profile.industry,
      profile.targetAudience,
      profile.brandVoice,
      profile.preferredCTA,
      profile.primaryColor,
      profile.secondaryColor,
      profile.website,
      profile.description,
    ].filter((value) => Boolean(value?.trim())).length;

    return Math.round((completedFields / 10) * 100);
  });
  readonly isBrandProfileComplete = computed(() => this.brandProfileCompletion() >= 80);
  readonly canManageCrew = computed(() =>
    this.hasAnyPermission(['CREW_VIEW', 'CREW_INVITE', 'CREW_UPDATE', 'CREW_REMOVE']),
  );
  readonly canEditWorkspace = computed(() =>
    this.hasAnyPermission(['WORKSPACE_UPDATE', 'WORKSPACE_SETTINGS_UPDATE']),
  );
  readonly canEditBrandProfile = computed(() =>
    this.hasAnyPermission(['BRAND_PROFILE_UPDATE']),
  );

  async loadWorkspaceDashboardContext(workspaceId?: string): Promise<void> {
    await this.loadWithWorkspaceContext(
      async (id) => {
        const [workspace, settings, brandProfile, crewMembers] = await Promise.all([
          firstValueFrom(this.workspaceService.getWorkspace(id, this.workspaceRequestContext())),
          firstValueFrom(this.workspaceService.getSettings(id, this.workspaceRequestContext())),
          firstValueFrom(this.brandProfileService.getBrandProfile(id, this.workspaceRequestContext())),
          firstValueFrom(this.crewService.listCrew(id, this.workspaceRequestContext())),
        ]);

        this.currentWorkspaceSignal.set(workspace);
        this.workspaceSettingsSignal.set(settings);
        this.brandProfileSignal.set(brandProfile);
        this.crewMembersSignal.set(crewMembers);
      },
      workspaceId,
    );
  }

  async loadWorkspaceSettingsContext(workspaceId?: string): Promise<void> {
    await this.loadWithWorkspaceContext(
      async (id) => {
        const [workspace, settings] = await Promise.all([
          firstValueFrom(this.workspaceService.getWorkspace(id, this.workspaceRequestContext())),
          firstValueFrom(this.workspaceService.getSettings(id, this.workspaceRequestContext())),
        ]);

        this.currentWorkspaceSignal.set(workspace);
        this.workspaceSettingsSignal.set(settings);
      },
      workspaceId,
    );
  }

  async loadBrandProfileContext(workspaceId?: string): Promise<void> {
    await this.loadWithWorkspaceContext(
      async (id) => {
        const [workspace, brandProfile] = await Promise.all([
          firstValueFrom(this.workspaceService.getWorkspace(id, this.workspaceRequestContext())),
          firstValueFrom(this.brandProfileService.getBrandProfile(id, this.workspaceRequestContext())),
        ]);

        this.currentWorkspaceSignal.set(workspace);
        this.brandProfileSignal.set(brandProfile);
      },
      workspaceId,
    );
  }

  async loadCrewContext(workspaceId?: string): Promise<void> {
    await this.loadWithWorkspaceContext(
      async (id) => {
        const [workspace, crewMembers] = await Promise.all([
          firstValueFrom(this.workspaceService.getWorkspace(id, this.workspaceRequestContext())),
          firstValueFrom(this.crewService.listCrew(id, this.workspaceRequestContext())),
        ]);

        this.currentWorkspaceSignal.set(workspace);
        this.crewMembersSignal.set(crewMembers);
      },
      workspaceId,
    );
  }

  async loadCrewReadonlyContext(workspaceId?: string): Promise<void> {
    await this.loadWithWorkspaceContext(
      async (id) => {
        const [workspace, brandProfile] = await Promise.all([
          firstValueFrom(this.workspaceService.getWorkspace(id, this.workspaceRequestContext())),
          firstValueFrom(this.brandProfileService.getBrandProfile(id, this.workspaceRequestContext())),
        ]);

        this.currentWorkspaceSignal.set(workspace);
        this.brandProfileSignal.set(brandProfile);
      },
      workspaceId,
    );
  }

  async loadAccessibleWorkspaces(): Promise<void> {
    await this.runLoader(async () => {
      const workspaces = await firstValueFrom(this.workspaceService.getMyWorkspaces());
      this.accessibleWorkspacesSignal.set(workspaces);
      this.syncWorkspaceContext(workspaces);
    });
  }

  selectWorkspace(workspaceId: string): void {
    const accessibleWorkspaces = this.accessibleWorkspacesSignal();
    if (
      accessibleWorkspaces.length &&
      !accessibleWorkspaces.some((workspace) => workspace.id === workspaceId)
    ) {
      this.workspaceErrorSignal.set('You no longer have access to this workspace.');
      return;
    }

    this.auth.setActiveWorkspaceId(workspaceId);
    this.workspaceErrorSignal.set(null);
    this.resetWorkspaceContext();
  }

  async saveWorkspaceSettings(payload: SaveWorkspaceSettingsPayload): Promise<WorkspaceActionResult> {
    const workspaceId = await this.resolveWorkspaceId();
    if (!workspaceId) {
      return this.missingWorkspaceResult();
    }

    try {
      this.workspaceLoadingSignal.set(true);
      this.workspaceErrorSignal.set(null);

      const settingsPayload = {
        ...payload.settings,
        notificationPreferences:
          payload.settings.notificationPreferences ?? DEFAULT_WORKSPACE_NOTIFICATION_PREFERENCES,
      };

      const [workspace, settings] = await Promise.all([
        firstValueFrom(this.workspaceService.updateWorkspace(workspaceId, payload.workspace)),
        firstValueFrom(this.workspaceService.updateSettings(workspaceId, settingsPayload)),
      ]);

      this.currentWorkspaceSignal.set(workspace);
      this.workspaceSettingsSignal.set(settings);
      this.notifications.success('Workspace updated', 'Workspace settings were saved.');

      return this.successResult();
    } catch (error) {
      return this.failureResult(error);
    } finally {
      this.workspaceLoadingSignal.set(false);
    }
  }

  async saveBrandProfile(payload: UpdateBrandProfilePayload): Promise<WorkspaceActionResult> {
    const workspaceId = await this.resolveWorkspaceId();
    if (!workspaceId) {
      return this.missingWorkspaceResult();
    }

    try {
      this.workspaceLoadingSignal.set(true);
      this.workspaceErrorSignal.set(null);

      const profile = await firstValueFrom(
        this.brandProfileService.updateBrandProfile(workspaceId, payload),
      );

      this.brandProfileSignal.set(profile);
      this.notifications.success('Brand profile updated', 'Brand context is ready for future AI flows.');

      return this.successResult();
    } catch (error) {
      return this.failureResult(error);
    } finally {
      this.workspaceLoadingSignal.set(false);
    }
  }

  async inviteCrew(payload: InviteCrewPayload): Promise<WorkspaceActionResult> {
    const workspaceId = await this.resolveWorkspaceId();
    if (!workspaceId) {
      return this.missingWorkspaceResult();
    }

    try {
      this.workspaceLoadingSignal.set(true);
      this.workspaceErrorSignal.set(null);

      await firstValueFrom(
        this.crewService.inviteCrew(workspaceId, {
          ...payload,
          permissions: normalizeCrewPermissions(payload.permissions),
        }),
      );

      const crewMembers = await firstValueFrom(this.crewService.listCrew(workspaceId));
      this.crewMembersSignal.set(crewMembers);
      this.notifications.success('Crew invited', 'The invite foundation was sent to the selected member.');

      return this.successResult();
    } catch (error) {
      return this.failureResult(error);
    } finally {
      this.workspaceLoadingSignal.set(false);
    }
  }

  async updateCrewMember(
    crewId: string,
    payload: UpdateCrewMemberPayload,
  ): Promise<WorkspaceActionResult> {
    const workspaceId = await this.resolveWorkspaceId();
    if (!workspaceId) {
      return this.missingWorkspaceResult();
    }

    try {
      this.workspaceLoadingSignal.set(true);
      this.workspaceErrorSignal.set(null);

      const crewMember = await firstValueFrom(
        this.crewService.updateCrewMember(workspaceId, crewId, {
          ...payload,
          permissions: normalizeCrewPermissions(payload.permissions),
        }),
      );

      this.crewMembersSignal.update((members) =>
        members.map((member) => (member.id === crewMember.id ? crewMember : member)),
      );
      this.notifications.success('Crew permissions updated', 'Crew access changes have been saved.');

      return this.successResult();
    } catch (error) {
      return this.failureResult(error);
    } finally {
      this.workspaceLoadingSignal.set(false);
    }
  }

  async removeCrewMember(crewId: string): Promise<WorkspaceActionResult> {
    const workspaceId = await this.resolveWorkspaceId();
    if (!workspaceId) {
      return this.missingWorkspaceResult();
    }

    try {
      this.workspaceLoadingSignal.set(true);
      this.workspaceErrorSignal.set(null);

      await firstValueFrom(this.crewService.removeCrewMember(workspaceId, crewId));

      this.crewMembersSignal.update((members) => members.filter((member) => member.id !== crewId));
      this.notifications.success('Crew member removed', 'Workspace access was revoked.');

      return this.successResult();
    } catch (error) {
      return this.failureResult(error);
    } finally {
      this.workspaceLoadingSignal.set(false);
    }
  }

  private async runLoader(operation: () => Promise<void>): Promise<void> {
    try {
      this.workspaceLoadingSignal.set(true);
      this.workspaceErrorSignal.set(null);
      await operation();
    } catch (error) {
      this.workspaceErrorSignal.set(this.mapWorkspaceLoadError(error));
    } finally {
      this.workspaceLoadingSignal.set(false);
    }
  }

  private async loadWithWorkspaceContext(
    operation: (workspaceId: string) => Promise<void>,
    explicitWorkspaceId?: string,
  ): Promise<void> {
    const workspaceId = await this.resolveWorkspaceId(explicitWorkspaceId);
    if (!workspaceId) {
      this.workspaceErrorSignal.set('Workspace context is unavailable.');
      return;
    }

    await this.runLoader(async () => {
      try {
        await operation(workspaceId);
      } catch (error) {
        if (!(await this.tryRecoverWorkspaceAccess(workspaceId, error, operation))) {
          throw error;
        }
      }
    });
  }

  private async resolveWorkspaceId(explicitWorkspaceId?: string): Promise<string | null> {
    const currentWorkspaceId =
      explicitWorkspaceId ?? this.currentWorkspaceSignal()?.id ?? this.auth.activeWorkspaceId();

    if (currentWorkspaceId) {
      return currentWorkspaceId;
    }

    return this.bootstrapWorkspaceId();
  }

  private async resolveFallbackWorkspaceId(currentWorkspaceId: string): Promise<string | null> {
    const workspaces = await this.fetchAccessibleWorkspaces();
    const fallbackWorkspace = workspaces.find((workspace) => workspace.id !== currentWorkspaceId);

    return fallbackWorkspace?.id ?? null;
  }

  private async bootstrapWorkspaceId(): Promise<string | null> {
    if (this.auth.currentRole() === 'MASTER') {
      return null;
    }

    const workspaces = await this.fetchAccessibleWorkspaces();
    if (workspaces.length !== 1) {
      return null;
    }

    const nextWorkspace = workspaces[0];

    if (!nextWorkspace) {
      return null;
    }

    this.auth.setActiveWorkspaceId(nextWorkspace.id);
    return nextWorkspace.id;
  }

  private async fetchAccessibleWorkspaces(): Promise<readonly WorkspaceSummary[]> {
    const workspaces = await firstValueFrom(
      this.workspaceService.getMyWorkspaces(this.workspaceRequestContext()),
    );
    this.accessibleWorkspacesSignal.set(workspaces);
    this.syncWorkspaceContext(workspaces);
    return workspaces;
  }

  private async tryRecoverWorkspaceAccess(
    currentWorkspaceId: string,
    error: unknown,
    operation: (workspaceId: string) => Promise<void>,
  ): Promise<boolean> {
    if (!this.shouldSwitchWorkspace(error)) {
      return false;
    }

    const fallbackWorkspaceId = await this.resolveFallbackWorkspaceId(currentWorkspaceId);
    if (!fallbackWorkspaceId) {
      return false;
    }

    this.selectWorkspace(fallbackWorkspaceId);
    this.notifications.info(
      'Workspace changed',
      'The previous workspace is no longer available. The next accessible workspace has been opened.',
    );
    await operation(fallbackWorkspaceId);
    return true;
  }

  private shouldSwitchWorkspace(error: unknown): boolean {
    const normalized = normalizeHttpError(error);
    return normalized.status === 403 || normalized.status === 404;
  }

  private syncWorkspaceContext(workspaces: readonly WorkspaceSummary[]): void {
    const activeWorkspaceId = this.auth.activeWorkspaceId();
    if (
      activeWorkspaceId &&
      !workspaces.some((workspace) => workspace.id === activeWorkspaceId) &&
      this.auth.currentRole() !== 'MASTER'
    ) {
      this.auth.setActiveWorkspaceId(null);
      this.resetWorkspaceContext();
    }

    if (
      !this.auth.activeWorkspaceId() &&
      this.auth.currentRole() !== 'MASTER' &&
      workspaces.length === 1
    ) {
      this.auth.setActiveWorkspaceId(workspaces[0].id);
    }
  }

  private resetWorkspaceContext(): void {
    this.currentWorkspaceSignal.set(null);
    this.workspaceSettingsSignal.set(null);
    this.brandProfileSignal.set(null);
    this.crewMembersSignal.set([]);
  }

  private hasAnyPermission(
    permissions: readonly (
      | 'CREW_VIEW'
      | 'CREW_INVITE'
      | 'CREW_UPDATE'
      | 'CREW_REMOVE'
      | 'WORKSPACE_UPDATE'
      | 'WORKSPACE_SETTINGS_UPDATE'
      | 'BRAND_PROFILE_UPDATE'
    )[],
  ): boolean {
    const availablePermissions =
      this.currentWorkspaceSignal()?.currentUserPermissions ?? this.auth.permissions();

    return permissions.some((permission) => availablePermissions.includes(permission));
  }

  private successResult(): WorkspaceActionResult {
    return { ok: true, fieldErrors: {} };
  }

  private missingWorkspaceResult(): WorkspaceActionResult {
    const message = 'Workspace context is unavailable.';
    this.workspaceErrorSignal.set(message);
    return { ok: false, message, fieldErrors: {} };
  }

  private failureResult(error: unknown): WorkspaceActionResult {
    const normalized = normalizeHttpError(error);
    this.workspaceErrorSignal.set(normalized.message);

    return {
      ok: false,
      message: normalized.message,
      fieldErrors: this.mapFieldErrors(normalized.errors),
    };
  }

  private mapFieldErrors(errors: readonly ApiError[]): Readonly<Record<string, string>> {
    return errors.reduce<Record<string, string>>((result, error) => {
      if (error.field) {
        result[error.field] = error.message;
      }
      return result;
    }, {});
  }

  private mapWorkspaceLoadError(error: unknown): string {
    const normalized = normalizeHttpError(error);

    if (normalized.status === 403) {
      return 'You no longer have access to this workspace.';
    }

    if (normalized.status === 404) {
      return 'The assigned workspace could not be found for this account.';
    }

    if (normalized.status >= 500 || normalized.message === 'Unexpected server error') {
      return 'Workspace data could not be loaded right now.';
    }

    return normalized.message;
  }

  private workspaceRequestContext(): HttpContext {
    return new HttpContext().set(SKIP_ERROR_TOAST, true);
  }
}
