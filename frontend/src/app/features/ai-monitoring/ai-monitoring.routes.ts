import { Routes } from '@angular/router';

export const AI_MONITORING_ROUTES: Routes = [
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
