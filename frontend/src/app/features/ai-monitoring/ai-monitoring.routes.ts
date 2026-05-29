import { Routes } from '@angular/router';

import { roleGuard } from '@app/core/guards/role.guard';

export const AI_MONITORING_ROUTES: Routes = [
  {
    path: 'master/creative-tools',
    canActivate: [roleGuard(['MASTER'])],
    loadComponent: () =>
      import('../master/ai-tools/master-ai-tools').then((m) => m.MasterAiToolsPage),
    data: { mode: 'tools' },
  },
  {
    path: 'master/creative-layers',
    canActivate: [roleGuard(['MASTER'])],
    loadComponent: () =>
      import('../master/ai-tools/master-ai-tools').then((m) => m.MasterAiToolsPage),
    data: { mode: 'layers' },
  },
  {
    path: 'master/provider-routing',
    canActivate: [roleGuard(['MASTER'])],
    loadComponent: () =>
      import('../master/ai-tools/master-ai-tools').then((m) => m.MasterAiToolsPage),
    data: { mode: 'routing' },
  },
  {
    path: 'master/provider-settings',
    canActivate: [roleGuard(['MASTER'])],
    loadComponent: () =>
      import('../master/ai-operations/master-ai-operations').then((m) => m.MasterAiOperationsPage),
    data: { mode: 'provider-settings' },
  },
  {
    path: 'master/provider-health',
    canActivate: [roleGuard(['MASTER'])],
    loadComponent: () =>
      import('../master/ai-operations/master-ai-operations').then((m) => m.MasterAiOperationsPage),
    data: { mode: 'provider-health' },
  },
  {
    path: 'master/layer-analytics',
    canActivate: [roleGuard(['MASTER'])],
    loadComponent: () =>
      import('../master/ai-operations/master-ai-operations').then((m) => m.MasterAiOperationsPage),
    data: { mode: 'layer-analytics' },
  },
  {
    path: 'master/ai-cost-usage',
    canActivate: [roleGuard(['MASTER'])],
    loadComponent: () =>
      import('../master/ai-operations/master-ai-operations').then((m) => m.MasterAiOperationsPage),
    data: { mode: 'ai-cost-usage' },
  },
  {
    path: 'master/ai-failures',
    canActivate: [roleGuard(['MASTER'])],
    loadComponent: () =>
      import('../master/ai-operations/master-ai-operations').then((m) => m.MasterAiOperationsPage),
    data: { mode: 'ai-failures' },
  },
  {
    path: 'ai-monitoring',
    loadComponent: () =>
      import('./dashboard/ai-monitoring-dashboard').then((m) => m.AiMonitoringDashboardPage),
  },
  {
    path: 'ai-monitoring/providers',
    loadComponent: () =>
      import('./provider-health/provider-health').then((m) => m.ProviderHealthPage),
  },
  {
    path: 'ai-monitoring/providers/metrics',
    loadComponent: () =>
      import('./provider-metrics/provider-metrics').then((m) => m.ProviderMetricsPage),
  },
  {
    path: 'ai-monitoring/layers',
    loadComponent: () =>
      import('./layer-analytics/layer-analytics').then((m) => m.LayerAnalyticsPage),
  },
  {
    path: 'ai-monitoring/workspaces',
    loadComponent: () =>
      import('./workspace-usage/workspace-ai-usage').then((m) => m.WorkspaceAiUsagePage),
  },
  {
    path: 'ai-monitoring/quality',
    loadComponent: () =>
      import('./quality-scores/quality-scores').then((m) => m.QualityScoresPage),
  },
  {
    path: 'ai-monitoring/failures',
    loadComponent: () =>
      import('./failures/ai-failures').then((m) => m.AiFailuresPage),
  },
];
