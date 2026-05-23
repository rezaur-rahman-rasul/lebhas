import { Routes } from '@angular/router';

import { RoleSectionPageComponent } from '@app/features/shared/role-section-page/role-section-page';

export const MASTER_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./master-home').then((m) => m.MasterHomeComponent),
  },
  {
    path: 'clients',
    component: RoleSectionPageComponent,
    data: {
      section: {
        eyebrow: 'Platform',
        title: 'Clients',
        description: 'Tenant and client-account onboarding surfaces start from the master boundary.',
        badgeLabel: 'MASTER',
        badgeTone: 'red',
        emptyIcon: 'folder-kanban',
        emptyTitle: 'Client management foundation',
        emptyDescription: 'Workspace portfolio controls are reserved for future implementation days.',
        highlights: [
          { title: 'Workspace registry', description: 'Cross-tenant visibility stays scoped to master users.' },
          { title: 'Onboarding flow', description: 'Workspace bootstrap and lifecycle boundaries are prepared.' },
          { title: 'Support operations', description: 'Client escalation and operational tooling will sit here.' },
        ],
      },
    },
  },
  {
    path: 'users',
    component: RoleSectionPageComponent,
    data: {
      section: {
        eyebrow: 'Platform',
        title: 'Users',
        description: 'Cross-workspace user oversight is prepared for master operators.',
        badgeLabel: 'MASTER',
        badgeTone: 'red',
        emptyIcon: 'users',
        emptyTitle: 'Global user operations',
        emptyDescription: 'User governance and audit tooling will expand from this route.',
        highlights: [
          { title: 'RBAC overview', description: 'Role-aware access is already enforced in the route shell.' },
          { title: 'Workspace membership', description: 'Tenant-scoped access remains isolated by workspace.' },
          { title: 'Audit readiness', description: 'Authentication and access events are ready for review surfaces.' },
        ],
      },
    },
  },
  {
    path: 'credits',
    component: RoleSectionPageComponent,
    data: {
      section: {
        eyebrow: 'Platform',
        title: 'Credits',
        description: 'Credit controls stay visible to master users while billing logic remains out of scope.',
        badgeLabel: 'MASTER',
        badgeTone: 'red',
        emptyIcon: 'credit-card',
        emptyTitle: 'Credit policy placeholder',
        emptyDescription: 'Subscription and credit administration will be implemented in later days.',
        highlights: [
          { title: 'Usage boundaries', description: 'Role-aware consumption surfaces can plug into this route.' },
          { title: 'Workspace quotas', description: 'Quota enforcement can be layered in without route churn.' },
          { title: 'Policy review', description: 'Platform finance controls stay separated from creative workflows.' },
        ],
      },
    },
  },
  {
    path: 'payments',
    component: RoleSectionPageComponent,
    data: {
      section: {
        eyebrow: 'Platform',
        title: 'Payments',
        description: 'Payment operations remain a reserved platform surface for future implementation.',
        badgeLabel: 'MASTER',
        badgeTone: 'red',
        emptyIcon: 'credit-card',
        emptyTitle: 'Payments are out of scope',
        emptyDescription: 'Day 2 stops at auth and tenant foundation, not billing execution.',
        highlights: [
          { title: 'Operational boundary', description: 'Payments stay segregated from workspace auth flows.' },
          { title: 'Audit readiness', description: 'Correlation IDs are already attached for future finance APIs.' },
          { title: 'Support shell', description: 'Route scaffolding is prepared without premature business logic.' },
        ],
      },
    },
  },
  {
    path: 'settings',
    component: RoleSectionPageComponent,
    data: {
      section: {
        eyebrow: 'Platform',
        title: 'Settings',
        description: 'Platform-level policy and security settings stay centralized for master access.',
        badgeLabel: 'MASTER',
        badgeTone: 'red',
        emptyIcon: 'settings',
        emptyTitle: 'Platform settings foundation',
        emptyDescription: 'Operational configuration panels can build on this route later.',
        highlights: [
          { title: 'Security defaults', description: 'JWT, refresh flow, and tenant headers are already configured.' },
          { title: 'Environment controls', description: 'Environment-specific API configuration is ready.' },
          { title: 'Access policy', description: 'RBAC route enforcement is already active for all protected areas.' },
        ],
      },
    },
  },
];
