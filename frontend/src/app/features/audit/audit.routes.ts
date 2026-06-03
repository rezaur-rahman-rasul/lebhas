import { Routes } from '@angular/router';

export const AUDIT_ROUTES: Routes = [
  {
    path: 'audit-logs',
    loadComponent: () => import('./logs/audit-logs').then((m) => m.AuditLogsPage),
  },
  {
    path: 'master/audit-logs',
    loadComponent: () => import('./logs/audit-logs').then((m) => m.AuditLogsPage),
  },
  {
    path: 'master/users-admins',
    loadComponent: () => import('./logs/audit-logs').then((m) => m.AuditLogsPage),
  },
];
