import { Routes } from '@angular/router';

export const ACTIVITY_ROUTES: Routes = [
  {
    path: 'activity-feed',
    loadComponent: () => import('./feed/activity-feed').then((m) => m.ActivityFeedPage),
  },
  {
    path: 'workspace-timeline',
    loadComponent: () =>
      import('./timeline/workspace-timeline').then((m) => m.WorkspaceTimelinePage),
  },
];
