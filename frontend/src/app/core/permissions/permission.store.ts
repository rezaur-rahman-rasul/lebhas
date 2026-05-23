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
import { Permission } from '@app/features/auth/models/user.models';

@Injectable({ providedIn: 'root' })
export class PermissionStore {
  private readonly auth = inject(CurrentUserStore);

  readonly role = this.auth.currentRole;
  readonly permissions = this.auth.permissions;

  private readonly promptPermissionContext = computed(() =>
    createPromptPermissionContext(this.role(), this.permissions()),
  );

  readonly canViewBrands = computed(() => this.auth.hasPermission('BRAND_VIEW'));
  readonly canManageBrands = computed(() => this.auth.hasPermission('BRAND_MANAGE'));
  readonly canViewProducts = computed(() => this.auth.hasPermission('PRODUCT_VIEW'));
  readonly canManageProducts = computed(() => this.auth.hasPermission('PRODUCT_MANAGE'));
  readonly canViewProjects = computed(() => this.auth.hasPermission('PROJECT_VIEW'));
  readonly canCreateProjects = computed(() => this.auth.hasPermission('PROJECT_CREATE'));
  readonly canUpdateProjects = computed(() => this.auth.hasPermission('PROJECT_UPDATE'));

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

  has(permission: Permission): boolean {
    return this.auth.hasPermission(permission);
  }
}
