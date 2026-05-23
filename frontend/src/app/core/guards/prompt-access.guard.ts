import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';

export const promptBuilderGuard: CanActivateFn = () => {
  const permissions = inject(PermissionStore);
  const router = inject(Router);

  if (permissions.canUsePromptBuilder()) {
    return true;
  }

  return router.createUrlTree(['/dashboard']);
};

export const promptHistoryGuard: CanActivateFn = () => {
  const permissions = inject(PermissionStore);
  const router = inject(Router);

  if (permissions.canViewPromptHistory()) {
    return true;
  }

  return router.createUrlTree(['/dashboard']);
};

export const promptTemplatesGuard: CanActivateFn = () => {
  const permissions = inject(PermissionStore);
  const router = inject(Router);

  if (permissions.canViewPromptTemplates()) {
    return true;
  }

  return router.createUrlTree(['/dashboard']);
};
