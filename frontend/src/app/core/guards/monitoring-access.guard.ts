import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';

export const masterMonitoringAccessGuard: CanActivateFn = () => {
  const permissions = inject(PermissionStore);
  const router = inject(Router);

  return permissions.canViewMasterMonitoring()
    ? true
    : router.createUrlTree(['/master/monitoring']);
};

export const systemHealthAccessGuard: CanActivateFn = () => {
  const permissions = inject(PermissionStore);
  const router = inject(Router);

  return permissions.canViewSystemHealth()
    ? true
    : router.createUrlTree(['/master/monitoring']);
};

export const monitoringAlertsAccessGuard: CanActivateFn = () => {
  const permissions = inject(PermissionStore);
  const router = inject(Router);

  return permissions.canViewMonitoringAlerts()
    ? true
    : router.createUrlTree(['/master/monitoring']);
};
