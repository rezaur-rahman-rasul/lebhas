import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';
import { DASHBOARD_ROUTES } from './features/dashboard/dashboard.routes';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./features/public/home/home').then((m) => m.HomeComponent),
    pathMatch: 'full',
    canActivate: [guestGuard],
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/public/home/home').then((m) => m.HomeComponent),
    canActivate: [guestGuard],
  },
  {
    path: '',
    loadComponent: () =>
      import('./core/layout/protected-layout/protected-layout').then(
        (m) => m.ProtectedLayoutComponent,
      ),
    canActivate: [authGuard],
    children: DASHBOARD_ROUTES,
  },
  {
    path: '**',
    redirectTo: '',
  },
];
