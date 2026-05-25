import { Injectable, computed, inject, signal } from '@angular/core';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import {
  FeatureLimit,
  PlanFeaturePolicy,
  WorkspaceContext,
  WorkspaceSubscription,
  WorkspaceSummary,
  WorkspaceUsageSummary,
} from './workspace.models';
import { WorkspaceApiService } from './workspace-api.service';

@Injectable({ providedIn: 'root' })
export class WorkspaceStore {
  private readonly auth = inject(CurrentUserStore);
  private readonly workspaceService = inject(WorkspaceApiService);

  private readonly workspacesSignal = signal<readonly WorkspaceSummary[]>([]);
  private readonly workspaceContextSignal = signal<WorkspaceContext | null>(null);
  private readonly loadingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);

  private initialized = false;
  private initializeInFlight: Promise<void> | null = null;

  readonly workspaces = this.workspacesSignal.asReadonly();
  readonly workspaceContext = this.workspaceContextSignal.asReadonly();
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
  readonly currentRole = computed(
    () => this.activeWorkspace()?.currentUserRole ?? this.auth.currentRole(),
  );
  readonly subscription = computed<WorkspaceSubscription | null>(
    () =>
      this.workspaceContextSignal()?.subscription ??
      this.activeWorkspace()?.subscription ??
      null,
  );
  readonly featurePolicy = computed<PlanFeaturePolicy | null>(
    () =>
      this.workspaceContextSignal()?.featurePolicy ??
      this.activeWorkspace()?.featurePolicy ??
      null,
  );
  readonly usage = computed<WorkspaceUsageSummary | null>(
    () => this.workspaceContextSignal()?.usage ?? this.activeWorkspace()?.usage ?? null,
  );
  readonly featureToggles = computed<Readonly<Record<string, boolean>>>(
    () =>
      this.workspaceContextSignal()?.featureToggles ??
      this.activeWorkspace()?.featureToggles ??
      {},
  );
  readonly activePlanLabel = computed(() => this.subscription()?.planName ?? 'Package details unavailable');
  readonly subscriptionStatusLabel = computed(() => this.subscription()?.status ?? 'Unknown');
  readonly remainingCreditsLabel = computed(() => {
    const credits = this.usage()?.creditsRemaining;
    return typeof credits === 'number' ? String(credits) : 'Usage details unavailable';
  });

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
      .then(() => this.loadWorkspaceContext(this.activeWorkspaceId()))
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
    this.workspaceContextSignal.set(null);
    void this.loadWorkspaceContext(workspaceId);
  }

  isFeatureEnabled(featureKey: string): boolean {
    if (this.featureToggles()[featureKey] === false) {
      return false;
    }

    const feature = this.featurePolicy()?.features?.[featureKey];
    return feature?.enabled !== false;
  }

  featureLimit(featureKey: string): FeatureLimit | null {
    return this.featurePolicy()?.limits?.[featureKey] ?? this.usage()?.limits?.[featureKey] ?? null;
  }

  featureMessage(featureKey: string): string | null {
    const feature = this.featurePolicy()?.features?.[featureKey];
    return feature?.reason ?? this.featureLimit(featureKey)?.message ?? null;
  }

  private async loadWorkspaceContext(workspaceId: string | null): Promise<void> {
    if (!workspaceId) {
      this.workspaceContextSignal.set(null);
      return;
    }

    try {
      const context = await this.workspaceService.getWorkspaceContext(workspaceId);
      this.workspaceContextSignal.set(context);
    } catch {
      this.workspaceContextSignal.set(null);
    }
  }
}
