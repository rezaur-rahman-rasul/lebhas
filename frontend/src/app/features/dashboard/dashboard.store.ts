import { Injectable, computed, inject, signal } from '@angular/core';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { PermissionStore } from '@app/core/permissions/permission.store';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { BrandStore } from '../brands/brand.store';
import { ProductServiceStore } from '../product-services/product-service.store';
import { ProjectStore } from '../projects/project.store';

@Injectable({ providedIn: 'root' })
export class DashboardStore {
  private readonly auth = inject(CurrentUserStore);
  private readonly permissions = inject(PermissionStore);
  private readonly workspace = inject(WorkspaceStore);
  private readonly brands = inject(BrandStore);
  private readonly products = inject(ProductServiceStore);
  private readonly projects = inject(ProjectStore);

  private readonly loadingSignal = signal(false);
  private readonly loadedWorkspaceIdSignal = signal<string | null>(null);
  private readonly errorSignal = signal<string | null>(null);

  readonly loading = this.loadingSignal.asReadonly();
  readonly loadedWorkspaceId = this.loadedWorkspaceIdSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();
  readonly ready = computed(() => {
    if (this.auth.currentRole() === 'MASTER') {
      return !this.loadingSignal();
    }

    return (
      !this.loadingSignal() &&
      Boolean(this.workspace.activeWorkspaceId()) &&
      this.loadedWorkspaceIdSignal() === this.workspace.activeWorkspaceId()
    );
  });
  readonly friendlyError = computed(() => this.errorSignal() || this.workspace.error());

  async load(workspaceId: string | null, options?: { readonly force?: boolean }): Promise<void> {
    if (this.auth.currentRole() === 'MASTER') {
      this.loadedWorkspaceIdSignal.set(workspaceId);
      this.errorSignal.set(null);
      return;
    }

    if (!workspaceId) {
      this.loadedWorkspaceIdSignal.set(null);
      this.errorSignal.set('We could not load your workspace. Please try again.');
      return;
    }

    if (!options?.force && this.loadedWorkspaceIdSignal() === workspaceId && !this.errorSignal()) {
      return;
    }

    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    try {
      await Promise.all([
        this.permissions.canViewBrands() ? this.brands.load(workspaceId, options) : Promise.resolve(),
        this.permissions.canViewProducts() ? this.products.load(workspaceId, options) : Promise.resolve(),
        this.permissions.canViewProjects() ? this.projects.load(workspaceId, options) : Promise.resolve(),
      ]);

      const firstError = this.brands.error() || this.products.error() || this.projects.error();
      this.errorSignal.set(firstError ? 'Dashboard details could not be loaded. Please try again.' : null);
      this.loadedWorkspaceIdSignal.set(workspaceId);
    } catch {
      this.errorSignal.set('Dashboard details could not be loaded. Please try again.');
      this.loadedWorkspaceIdSignal.set(workspaceId);
    } finally {
      this.loadingSignal.set(false);
    }
  }

  async retry(): Promise<void> {
    await this.load(this.workspace.activeWorkspaceId(), { force: true });
  }
}
