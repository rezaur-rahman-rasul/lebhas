import { Routes } from '@angular/router';

export const AUDIT_ROUTES: Routes = [
  {
    path: 'audit-logs',
    loadComponent: () => import('./logs/audit-logs').then((m) => m.AuditLogsPage),
  },
];
