import { Routes } from '@angular/router';

import {
  creditLedgerAccessGuard,
  downloadUsageAccessGuard,
  masterUsageAccessGuard,
  planUtilizationAccessGuard,
  shareUsageAccessGuard,
  usageBillingAccessGuard,
} from '@app/core/guards/usage-billing-access.guard';

export const USAGE_BILLING_ROUTES: Routes = [
  {
    path: 'master/workspaces',
    canActivate: [masterUsageAccessGuard],
    loadComponent: () =>
      import('./master-usage/master-usage-overview').then((m) => m.MasterUsageOverviewPage),
    data: { section: 'workspaces' },
  },
  {
    path: 'credit-usage',
    loadComponent: () =>
      import('./dashboard/usage-billing-dashboard').then(
        (m) => m.UsageBillingDashboardPage,
      ),
  },
  {
    path: 'usage-billing',
    loadComponent: () =>
      import('./dashboard/usage-billing-dashboard').then(
        (m) => m.UsageBillingDashboardPage,
      ),
  },
  {
    path: 'usage-billing/credits',
    canActivate: [creditLedgerAccessGuard],
    loadComponent: () =>
      import('./credit-ledger/credit-ledger').then((m) => m.CreditLedgerPage),
  },
  {
    path: 'usage-billing/logs',
    canActivate: [usageBillingAccessGuard],
    loadComponent: () =>
      import('./usage-logs/usage-billing-logs').then((m) => m.UsageBillingLogsPage),
  },
  {
    path: 'usage-billing/downloads',
    canActivate: [downloadUsageAccessGuard],
    loadComponent: () =>
      import('./download-usage/download-usage').then((m) => m.DownloadUsagePage),
  },
  {
    path: 'usage-billing/shares',
    canActivate: [shareUsageAccessGuard],
    loadComponent: () =>
      import('./share-usage/share-usage').then((m) => m.ShareUsagePage),
  },
  {
    path: 'usage-billing/monthly-snapshots',
    canActivate: [usageBillingAccessGuard],
    loadComponent: () =>
      import('./monthly-snapshots/monthly-usage-snapshots').then(
        (m) => m.MonthlyUsageSnapshotsPage,
      ),
  },
  {
    path: 'master/usage-overview',
    canActivate: [masterUsageAccessGuard],
    loadComponent: () =>
      import('./master-usage/master-usage-overview').then((m) => m.MasterUsageOverviewPage),
    data: { section: 'overview' },
  },
  {
    path: 'master/workspace-usage',
    canActivate: [masterUsageAccessGuard],
    loadComponent: () =>
      import('./master-usage/master-usage-overview').then((m) => m.MasterUsageOverviewPage),
    data: { section: 'workspaces' },
  },
  {
    path: 'master/plan-utilization',
    canActivate: [planUtilizationAccessGuard],
    loadComponent: () =>
      import('./master-usage/master-usage-overview').then((m) => m.MasterUsageOverviewPage),
    data: { section: 'plan-utilization' },
  },
  {
    path: 'master/usage',
    loadComponent: () =>
      import('./master-usage/master-usage-overview').then((m) => m.MasterUsageOverviewPage),
    data: { section: 'overview' },
  },
  {
    path: 'master/usage/workspaces',
    canActivate: [masterUsageAccessGuard],
    loadComponent: () =>
      import('./master-usage/master-usage-overview').then((m) => m.MasterUsageOverviewPage),
    data: { section: 'workspaces' },
  },
  {
    path: 'master/usage/ai-costs',
    canActivate: [masterUsageAccessGuard],
    loadComponent: () =>
      import('./master-usage/master-usage-overview').then((m) => m.MasterUsageOverviewPage),
    data: { section: 'ai-costs' },
  },
  {
    path: 'master/usage/plan-utilization',
    canActivate: [planUtilizationAccessGuard],
    loadComponent: () =>
      import('./master-usage/master-usage-overview').then((m) => m.MasterUsageOverviewPage),
    data: { section: 'plan-utilization' },
  },
];
