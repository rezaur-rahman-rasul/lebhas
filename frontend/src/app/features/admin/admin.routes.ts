import { Routes } from '@angular/router';

import { RoleSectionPageComponent } from '@app/features/shared/role-section-page/role-section-page';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./workspace/pages/workspace-dashboard/workspace-dashboard-page').then(
        (m) => m.WorkspaceDashboardPageComponent,
      ),
  },
  {
    path: 'team',
    pathMatch: 'full',
    redirectTo: 'crew',
  },
  {
    path: 'settings',
    loadComponent: () =>
      import('./workspace/pages/workspace-settings/workspace-settings-page').then(
        (m) => m.WorkspaceSettingsPageComponent,
      ),
  },
  {
    path: 'crew',
    loadComponent: () =>
      import('./workspace/pages/crew-management/crew-management-page').then(
        (m) => m.CrewManagementPageComponent,
      ),
  },
  {
    path: 'brand-profile',
    loadComponent: () =>
      import('./workspace/pages/brand-profile/brand-profile-page').then(
        (m) => m.BrandProfilePageComponent,
      ),
  },
  {
    path: 'assets',
    loadChildren: () => import('./assets/assets.routes').then((m) => m.ASSET_ROUTES),
  },
  {
    path: 'prompts',
    loadChildren: () => import('./prompts/prompts.routes').then((m) => m.PROMPT_ROUTES),
  },
  {
    path: 'creative-generation',
    loadChildren: () =>
      import('./creative-generation/creative-generation.routes').then(
        (m) => m.CREATIVE_GENERATION_ROUTES,
      ),
  },
  {
    path: 'creatives',
    component: RoleSectionPageComponent,
    data: {
      section: {
        eyebrow: 'Workspace',
        title: 'Creatives',
        description: 'Creative generation UI remains a future module, but the workspace shell is ready.',
        badgeLabel: 'ADMIN',
        badgeTone: 'brand',
        emptyIcon: 'wand-sparkles',
        emptyTitle: 'Creative generation comes later',
        emptyDescription: 'Day 2 stops at auth, RBAC, and tenant foundations.',
        highlights: [
          { title: 'Role protection', description: 'Only permitted roles can reach creative routes.' },
          { title: 'Session continuity', description: 'Refresh token handling is already prepared for longer sessions.' },
          { title: 'Workspace scope', description: 'Tenant-aware headers are already attached to requests.' },
        ],
      },
    },
  },
  {
    path: 'billing',
    component: RoleSectionPageComponent,
    data: {
      section: {
        eyebrow: 'Workspace',
        title: 'Billing',
        description: 'Billing remains a future module, but navigation and route ownership are defined.',
        badgeLabel: 'ADMIN',
        badgeTone: 'brand',
        emptyIcon: 'credit-card',
        emptyTitle: 'Billing placeholder',
        emptyDescription: 'Payment and billing UI are explicitly out of scope for Day 2.',
        highlights: [
          { title: 'Access shell', description: 'Admin-only navigation keeps workspace billing isolated.' },
          { title: 'Session policy', description: 'Logout and refresh token flows are already wired.' },
          { title: 'Future integration', description: 'The route can accept billing UI later without auth rewrites.' },
        ],
      },
    },
  },
];
