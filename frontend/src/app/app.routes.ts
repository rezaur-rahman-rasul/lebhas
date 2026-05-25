import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';
import { DASHBOARD_ROUTES } from './features/dashboard/dashboard.routes';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./features/public/home/home').then((m) => m.HomeComponent),
    pathMatch: 'full',
  },
  {
    path: '',
    loadComponent: () =>
      import('./shared/layouts/dashboard-layout/dashboard-layout').then(
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
