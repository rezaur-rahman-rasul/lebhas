import { Injectable, computed, inject, signal } from '@angular/core';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { WorkspaceSummary } from './workspace.models';
import { WorkspaceService } from './workspace.service';

@Injectable({ providedIn: 'root' })
export class WorkspaceStore {
  private readonly auth = inject(CurrentUserStore);
  private readonly workspaceService = inject(WorkspaceService);

  private readonly workspacesSignal = signal<readonly WorkspaceSummary[]>([]);
  private readonly loadingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);

  private initialized = false;
  private initializeInFlight: Promise<void> | null = null;

  readonly workspaces = this.workspacesSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();
  readonly activeWorkspaceId = this.auth.activeWorkspaceId;
  readonly hasWorkspaceSelection = computed(() => Boolean(this.activeWorkspaceId()));
  readonly activeWorkspace = computed(() => {
    const activeWorkspaceId = this.activeWorkspaceId();
    return this.workspacesSignal().find((workspace) => workspace.id === activeWorkspaceId) ?? null;
  });
  readonly workspaceLabel = computed(
    () =>
      this.activeWorkspace()?.name ??
      this.auth.currentUser()?.workspaceName ??
      this.auth.currentUser()?.workspace.id ??
      'Select workspace',
  );

  async initialize(): Promise<void> {
    if (this.initialized || !this.auth.isAuthenticated()) {
      return;
    }

    if (this.initializeInFlight) {
      return this.initializeInFlight;
    }

    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    this.initializeInFlight = this.workspaceService
      .getAccessibleWorkspaces()
      .then((workspaces) => {
        this.workspacesSignal.set(workspaces);

        const activeWorkspaceId = this.auth.activeWorkspaceId();
        if (activeWorkspaceId && workspaces.some((workspace) => workspace.id === activeWorkspaceId)) {
          return;
        }

        if (this.auth.currentRole() === 'MASTER') {
          return;
        }

        const currentUserWorkspaceId = this.auth.currentUser()?.workspace.id ?? null;
        const fallbackWorkspaceId = currentUserWorkspaceId ?? workspaces[0]?.id ?? null;
        this.auth.setActiveWorkspaceId(fallbackWorkspaceId);
      })
      .catch(() => {
        this.errorSignal.set('Workspace context could not be loaded.');
      })
      .finally(() => {
        this.loadingSignal.set(false);
        this.initialized = true;
        this.initializeInFlight = null;
      });

    return this.initializeInFlight;
  }

  setActiveWorkspaceId(workspaceId: string | null): void {
    this.auth.setActiveWorkspaceId(workspaceId);
  }
}
