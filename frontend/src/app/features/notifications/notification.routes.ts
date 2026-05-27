import { Routes } from '@angular/router';

import { notificationPreferencesAccessGuard } from '@app/core/guards/notification-access.guard';

export const NOTIFICATION_ROUTES: Routes = [
  {
    path: 'notifications',
    loadComponent: () =>
      import('./center/notification-center').then((m) => m.NotificationCenterPage),
  },
  {
    path: 'notifications/preferences',
    canActivate: [notificationPreferencesAccessGuard],
    loadComponent: () =>
      import('./preferences/notification-preferences').then(
        (m) => m.NotificationPreferencesPage,
      ),
  },
];
