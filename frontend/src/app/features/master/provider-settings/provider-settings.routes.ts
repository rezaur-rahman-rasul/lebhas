import { Routes } from '@angular/router';
import { roleGuard } from '@app/core/guards/role.guard';

export const MASTER_PROVIDER_SETTINGS_ROUTES: Routes = [
  {
    path: 'master/provider-settings',
    canActivate: [roleGuard(['MASTER'])],
    loadComponent: () => import('./pages/provider-settings-page/provider-settings-page').then((m) => m.ProviderSettingsPage),
  },
  {
    path: 'master/provider-credit-pools',
    canActivate: [roleGuard(['MASTER'])],
    loadComponent: () => import('./pages/provider-credit-pool-page/provider-credit-pool-page').then((m) => m.ProviderCreditPoolPage),
  },
  {
    path: 'master/exchange-policies',
    canActivate: [roleGuard(['MASTER'])],
    loadComponent: () => import('./pages/provider-exchange-policy-page/provider-exchange-policy-page').then((m) => m.ProviderExchangePolicyPage),
  },
  {
    path: 'master/credit-overview',
    canActivate: [roleGuard(['MASTER'])],
    loadComponent: () => import('./pages/master-credit-overview-page/master-credit-overview-page').then((m) => m.MasterCreditOverviewPage),
  },
];
