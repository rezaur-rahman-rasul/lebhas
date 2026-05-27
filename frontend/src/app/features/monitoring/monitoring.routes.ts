import { Routes } from '@angular/router';

import {
  masterMonitoringAccessGuard,
  monitoringAlertsAccessGuard,
  systemHealthAccessGuard,
} from '@app/core/guards/monitoring-access.guard';

export const MONITORING_ROUTES: Routes = [
  {
    path: 'monitoring',
    redirectTo: 'master/monitoring',
    pathMatch: 'full',
  },
  {
    path: 'monitoring/system-health',
    redirectTo: 'master/monitoring/system-health',
    pathMatch: 'full',
  },
  {
    path: 'monitoring/alerts',
    redirectTo: 'master/monitoring/alerts',
    pathMatch: 'full',
  },
  {
    path: 'master/monitoring',
    loadComponent: () =>
      import('./dashboard/master-monitoring-dashboard').then(
        (m) => m.MasterMonitoringDashboardPage,
      ),
  },
  {
    path: 'master/monitoring/alerts',
    canActivate: [monitoringAlertsAccessGuard],
    loadComponent: () => import('./alerts/monitoring-alerts').then((m) => m.MonitoringAlertsPage),
  },
  {
    path: 'master/monitoring/system-health',
    canActivate: [systemHealthAccessGuard],
    loadComponent: () => import('./system-health/system-health').then((m) => m.SystemHealthPage),
  },
  {
    path: 'master/monitoring/ai-providers',
    canActivate: [masterMonitoringAccessGuard],
    loadComponent: () =>
      import('./dashboard/master-monitoring-dashboard').then(
        (m) => m.MasterMonitoringDashboardPage,
      ),
  },
  {
    path: 'master/monitoring/payments',
    canActivate: [masterMonitoringAccessGuard],
    loadComponent: () =>
      import('./dashboard/master-monitoring-dashboard').then(
        (m) => m.MasterMonitoringDashboardPage,
      ),
  },
  {
    path: 'master/monitoring/workspaces',
    canActivate: [masterMonitoringAccessGuard],
    loadComponent: () =>
      import('./dashboard/master-monitoring-dashboard').then(
        (m) => m.MasterMonitoringDashboardPage,
      ),
  },
];
