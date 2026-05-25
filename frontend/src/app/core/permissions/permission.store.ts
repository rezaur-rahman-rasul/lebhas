import { Injectable, computed, inject } from '@angular/core';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import {
  canEnhancePrompt,
  canManageTemplates,
  canUsePromptBuilder,
  canViewPromptHistory,
  canViewPromptTemplates,
  createPromptPermissionContext,
} from '@app/core/permissions/prompt.permissions';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { Permission } from '@app/features/auth/models/user.models';

interface PermissionCheckOptions {
  readonly feature?: string;
  readonly requireActiveSubscription?: boolean;
}

@Injectable({ providedIn: 'root' })
export class PermissionStore {
  private readonly auth = inject(CurrentUserStore);
  private readonly workspace = inject(WorkspaceStore);

  readonly role = this.auth.currentRole;
  readonly permissions = this.auth.permissions;
  readonly subscription = this.workspace.subscription;
  readonly featurePolicy = this.workspace.featurePolicy;

  private readonly promptPermissionContext = computed(() =>
    createPromptPermissionContext(this.role(), this.permissions()),
  );

  readonly hasActiveSubscription = computed(() => {
    const status = this.subscription()?.status?.toLowerCase();
    return !status || ['active', 'trialing'].includes(status);
  });

  readonly canViewBrands = computed(() => this.has('BRAND_VIEW', { feature: 'brands' }));
  readonly canManageBrands = computed(() => this.has('BRAND_MANAGE', { feature: 'brands.manage' }));
  readonly canViewProducts = computed(() => this.has('PRODUCT_VIEW', { feature: 'products' }));
  readonly canManageProducts = computed(() =>
    this.has('PRODUCT_MANAGE', { feature: 'products.manage' }),
  );
  readonly canViewProjects = computed(() => this.has('PROJECT_VIEW', { feature: 'projects' }));
  readonly canCreateProjects = computed(() =>
    this.has('PROJECT_CREATE', { feature: 'projects.create' }),
  );
  readonly canUpdateProjects = computed(() =>
    this.has('PROJECT_UPDATE', { feature: 'projects.update' }),
  );

  readonly canUsePromptBuilder = computed(() =>
    canUsePromptBuilder(this.promptPermissionContext()),
  );
  readonly canEnhancePrompt = computed(() => canEnhancePrompt(this.promptPermissionContext()));
  readonly canManageTemplates = computed(() => canManageTemplates(this.promptPermissionContext()));
  readonly canViewPromptTemplates = computed(() =>
    canViewPromptTemplates(this.promptPermissionContext()),
  );
  readonly canViewPromptHistory = computed(() =>
    canViewPromptHistory(this.promptPermissionContext()),
  );

  has(permission: Permission, options?: PermissionCheckOptions): boolean {
    if (!this.auth.hasPermission(permission)) {
      return false;
    }

    if (options?.requireActiveSubscription && !this.hasActiveSubscription()) {
      return false;
    }

    return options?.feature ? this.workspace.isFeatureEnabled(options.feature) : true;
  }

  canUseFeature(featureKey: string, options?: Omit<PermissionCheckOptions, 'feature'>): boolean {
    if (options?.requireActiveSubscription && !this.hasActiveSubscription()) {
      return false;
    }

    return this.workspace.isFeatureEnabled(featureKey);
  }

  featureDisabledMessage(featureKey: string): string | null {
    return this.workspace.featureMessage(featureKey);
  }
}
