import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';

export const usageBillingAccessGuard: CanActivateFn = () => {
  const permissions = inject(PermissionStore);
  const router = inject(Router);

  return permissions.canViewUsageBilling() ? true : router.createUrlTree(['/usage-billing']);
};

export const creditLedgerAccessGuard: CanActivateFn = () => {
  const permissions = inject(PermissionStore);
  const router = inject(Router);

  return permissions.canViewCreditLedger() ? true : router.createUrlTree(['/usage-billing']);
};

export const downloadUsageAccessGuard: CanActivateFn = () => {
  const permissions = inject(PermissionStore);
  const router = inject(Router);

  return permissions.canViewDownloadUsage() ? true : router.createUrlTree(['/usage-billing']);
};

export const shareUsageAccessGuard: CanActivateFn = () => {
  const permissions = inject(PermissionStore);
  const router = inject(Router);

  return permissions.canViewShareUsage() ? true : router.createUrlTree(['/usage-billing']);
};

export const masterUsageAccessGuard: CanActivateFn = () => {
  const permissions = inject(PermissionStore);
  const router = inject(Router);

  return permissions.canViewMasterUsage() ? true : router.createUrlTree(['/master/usage']);
};

export const planUtilizationAccessGuard: CanActivateFn = () => {
  const permissions = inject(PermissionStore);
  const router = inject(Router);

  return permissions.canViewPlanUtilization() ? true : router.createUrlTree(['/master/usage']);
};
