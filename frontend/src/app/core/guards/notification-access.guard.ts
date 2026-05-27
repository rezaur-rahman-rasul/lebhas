import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';

export const notificationAccessGuard: CanActivateFn = () => {
  const permissions = inject(PermissionStore);
  const router = inject(Router);

  return permissions.canViewNotifications() ? true : router.createUrlTree(['/notifications']);
};

export const notificationPreferencesAccessGuard: CanActivateFn = () => {
  const permissions = inject(PermissionStore);
  const router = inject(Router);

  return permissions.canManageNotificationPreferences()
    ? true
    : router.createUrlTree(['/notifications']);
};
