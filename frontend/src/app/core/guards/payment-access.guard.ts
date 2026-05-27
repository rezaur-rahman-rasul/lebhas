import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';

export const paymentDashboardAccessGuard: CanActivateFn = () => {
  const permissions = inject(PermissionStore);
  const router = inject(Router);

  return canAccessPayments(permissions) ? true : router.createUrlTree(['/payments']);
};

export const subscriptionPurchaseAccessGuard: CanActivateFn = () => {
  const permissions = inject(PermissionStore);
  const router = inject(Router);

  return permissions.canPurchaseSubscription() ? true : router.createUrlTree(['/payments']);
};

export const creditPurchaseAccessGuard: CanActivateFn = () => {
  const permissions = inject(PermissionStore);
  const router = inject(Router);

  return permissions.canPurchaseCredits() ? true : router.createUrlTree(['/payments']);
};

export const paymentTransactionsAccessGuard: CanActivateFn = () => {
  const permissions = inject(PermissionStore);
  const router = inject(Router);

  return permissions.canViewPayments() ? true : router.createUrlTree(['/payments']);
};

export const invoicesAccessGuard: CanActivateFn = () => {
  const permissions = inject(PermissionStore);
  const router = inject(Router);

  return permissions.canViewInvoices() ? true : router.createUrlTree(['/payments']);
};

export const paymentProviderManagementAccessGuard: CanActivateFn = () => {
  const permissions = inject(PermissionStore);
  const router = inject(Router);

  return permissions.canManagePaymentProviders() ? true : router.createUrlTree(['/payments']);
};

export const creditPackageManagementAccessGuard: CanActivateFn = () => {
  const permissions = inject(PermissionStore);
  const router = inject(Router);

  return permissions.canManageCreditPackages() ? true : router.createUrlTree(['/payments']);
};

function canAccessPayments(permissions: PermissionStore): boolean {
  return (
    permissions.canPurchaseSubscription() ||
    permissions.canPurchaseCredits() ||
    permissions.canViewPayments() ||
    permissions.canViewInvoices() ||
    permissions.canManagePaymentProviders() ||
    permissions.canManageCreditPackages()
  );
}
