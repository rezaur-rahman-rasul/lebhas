import { Routes } from '@angular/router';

import { promptBuilderGuard } from '@app/core/guards/prompt-access.guard';

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
    path: 'projects/:projectId/prompts',
    canActivate: [promptBuilderGuard],
    loadComponent: () =>
      import('../prompts/builder/prompt-builder').then((m) => m.PromptBuilderPage),
  },
  sectionRoute('projects/:projectId/creative-requests', {
    eyebrow: 'Projects / Campaigns',
    title: 'Creative Requests',
    description: 'Project-scoped creative request intake is reserved behind the campaign context.',
    badge: 'ADMIN / CREW',
    badgeTone: 'blue',
    emptyIcon: 'pencil-line',
    emptyTitle: 'Creative request intake is staged',
    emptyDescription: 'Use this route as the stable campaign-level entry point when Day 6 request flows are consolidated.',
    cards: [
      { title: 'Campaign context', description: 'Requests stay anchored to the selected project campaign.' },
      { title: 'Asset readiness', description: 'Raw product media and prompt context remain upstream of request creation.' },
      { title: 'Review path', description: 'Generated versions and approval workflow follow after request intake.' },
    ],
  }),
  sectionRoute('creative-requests/:creativeRequestId', {
    eyebrow: 'Shared',
    title: 'Creative Request Detail',
    description: 'Stable detail route for one creative request inside the active workspace.',
    badge: 'ADMIN / CREW',
    badgeTone: 'blue',
    emptyIcon: 'pencil-line',
    emptyTitle: 'Request detail consolidation is staged',
    emptyDescription: 'Existing Day 1 to Day 6 implementation can converge here without adding another request page.',
    cards: [
      { title: 'Brief context', description: 'Keep campaign, product, and prompt inputs visible on the request detail.' },
      { title: 'Generation handoff', description: 'Generated versions remain downstream of the selected request.' },
      { title: 'Approval readiness', description: 'Approval state can attach here when backend availability allows it.' },
    ],
  }),
  sectionRoute('generated-versions/:generatedVersionId', {
    eyebrow: 'Shared',
    title: 'Generated Version Detail',
    description: 'Stable detail route for one generated creative version.',
    badge: 'ADMIN / CREW',
    badgeTone: 'blue',
    emptyIcon: 'image',
    emptyTitle: 'Generated version detail is staged',
    emptyDescription: 'The route is reserved for existing generated output detail work without creating future modules.',
    cards: [
      { title: 'Creative output', description: 'A generated version belongs to a creative request.' },
      { title: 'Approval state', description: 'Backend-provided approval availability should drive actions here.' },
      { title: 'Export readiness', description: 'Downloads and sharing remain backend-context dependent.' },
    ],
  }),
  sectionRoute('approvals', {
    eyebrow: 'Approval Workflow',
    title: 'Approvals',
    description: 'Workspace-level approval queue route for existing review flow consolidation.',
    badge: 'ADMIN / CREW',
    badgeTone: 'blue',
    emptyIcon: 'shield-check',
    emptyTitle: 'Approval workflow is staged',
    emptyDescription: 'Approval availability must come from backend context before actions are exposed.',
    cards: [
      { title: 'Review queue', description: 'Collect generated versions that require workspace approval.' },
      { title: 'Permission-aware actions', description: 'Show approve, reject, share, and download actions only from backend capabilities.' },
      { title: 'Current context', description: 'Keep workspace, brand, project, request, and version context visible.' },
    ],
  }),
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'dashboard',
  },
];
