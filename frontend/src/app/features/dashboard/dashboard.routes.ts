import { Routes } from '@angular/router';

import {
  promptBuilderGuard,
  promptHistoryGuard,
  promptTemplatesGuard,
} from '@app/core/guards/prompt-access.guard';

function sectionRoute(
  path: string,
  section: {
    readonly eyebrow: string;
    readonly title: string;
    readonly description: string;
    readonly badge: string;
    readonly badgeTone: 'neutral' | 'brand' | 'blue' | 'red';
    readonly emptyIcon: string;
    readonly emptyTitle: string;
    readonly emptyDescription: string;
    readonly cards: readonly { readonly title: string; readonly description: string }[];
  },
): Routes[number] {
  return {
    path,
    loadComponent: () =>
      import('../workspace/section/section').then((m) => m.WorkspaceSectionPageComponent),
    data: { section },
  };
}

export const DASHBOARD_ROUTES: Routes = [
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./overview/overview').then((m) => m.DashboardOverviewComponent),
  },
  {
    path: 'brands',
    loadComponent: () => import('../brands/brands').then((m) => m.BrandsComponent),
  },
  {
    path: 'product-services',
    loadComponent: () =>
      import('../product-services/product-services').then((m) => m.ProductServicesComponent),
  },
  {
    path: 'projects',
    loadComponent: () => import('../projects/projects').then((m) => m.ProjectsComponent),
  },
  {
    path: 'projects/:projectId/assets',
    loadComponent: () =>
      import('../assets/pages/project-assets/project-assets').then((m) => m.ProjectAssetsPage),
  },
  {
    path: 'projects/:projectId/prompts/history',
    canActivate: [promptHistoryGuard],
    loadComponent: () =>
      import('../prompts/history/prompt-history').then((m) => m.PromptHistoryPage),
  },
  {
    path: 'projects/:projectId/prompts',
    canActivate: [promptBuilderGuard],
    loadComponent: () =>
      import('../prompts/builder/prompt-builder').then((m) => m.PromptBuilderPage),
  },
  {
    path: 'prompt-templates',
    canActivate: [promptTemplatesGuard],
    loadComponent: () =>
      import('../prompts/templates/prompt-templates').then((m) => m.PromptTemplatesPage),
  },
  {
    path: 'assets',
    loadChildren: () => import('../assets/assets.routes').then((m) => m.ASSET_ROUTES),
  },
  sectionRoute('workspaces', {
    eyebrow: 'Master',
    title: 'Workspaces',
    description: 'Cross-tenant workspace access remains ready for a later master operations module.',
    badge: 'MASTER',
    badgeTone: 'red',
    emptyIcon: 'building-2',
    emptyTitle: 'Workspace management stays out of Day 2',
    emptyDescription: 'The route exists so the master shell does not need restructuring when tenant operations land.',
    cards: [
      { title: 'Tenant roster', description: 'Prepare to list client and internal workspaces from one master surface.' },
      { title: 'Support access', description: 'Keep master-level access ready for support and tenant handoff workflows.' },
      { title: 'Workspace switching', description: 'The header already persists the active workspace selection.' },
    ],
  }),
  sectionRoute('users', {
    eyebrow: 'Master',
    title: 'Users',
    description: 'Foundation route for platform-wide user access and support visibility.',
    badge: 'MASTER',
    badgeTone: 'red',
    emptyIcon: 'users',
    emptyTitle: 'User administration stays outside Day 2',
    emptyDescription: 'Navigation and access shape are ready, but user lifecycle modules are intentionally deferred.',
    cards: [
      { title: 'Admin roster', description: 'Prepare a clean list of workspace admins and system users.' },
      { title: 'Role controls', description: 'Keep room for master, admin, and crew governance.' },
      { title: 'Audit trail', description: 'Reserve space for future support and security actions.' },
    ],
  }),
  sectionRoute('support-access', {
    eyebrow: 'Master',
    title: 'Support Access',
    description: 'Protected placeholder for support entry into tenant workspaces.',
    badge: 'MASTER',
    badgeTone: 'red',
    emptyIcon: 'life-buoy',
    emptyTitle: 'Support access flows come later',
    emptyDescription: 'This route exists so the shell and role map do not need redesign when support tooling lands.',
    cards: [
      { title: 'Escalation context', description: 'Track which tenant a support session is acting inside.' },
      { title: 'Read-only defaults', description: 'Leave room for future restrictions before write access is allowed.' },
      { title: 'Session logging', description: 'Coordinate with backend security and audit events later.' },
    ],
  }),
  sectionRoute('creative-requests', {
    eyebrow: 'Shared',
    title: 'Creative Requests',
    description: 'Entry point for generation jobs and review queues inside the active workspace.',
    badge: 'ADMIN / CREW',
    badgeTone: 'blue',
    emptyIcon: 'pencil-line',
    emptyTitle: 'Request management is staged for later work',
    emptyDescription: 'This foundation keeps the route ready while generation UI and approval flows stay out of Day 2.',
    cards: [
      { title: 'Brief intake', description: 'Prepare a clear path from campaign brief to creative request.' },
      { title: 'Assignment model', description: 'Support crew-level execution after routing logic arrives.' },
      { title: 'Review readiness', description: 'Leave hooks for generated versions and approval states.' },
    ],
  }),
  sectionRoute('generated-versions', {
    eyebrow: 'Shared',
    title: 'Generated Versions',
    description: 'Placeholder route for generated creative output lists and variant history.',
    badge: 'ADMIN / CREW',
    badgeTone: 'blue',
    emptyIcon: 'image',
    emptyTitle: 'Generated output galleries are not in Day 2',
    emptyDescription: 'The shell now reserves a stable place for version browsing and export controls later.',
    cards: [
      { title: 'Variant history', description: 'Generated versions will sit under creative requests in the hierarchy.' },
      { title: 'Approval states', description: 'The route can later reflect pending, approved, and rejected variants.' },
      { title: 'Export readiness', description: 'Downloads and sharing will connect here in later milestones.' },
    ],
  }),
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'dashboard',
  },
];
