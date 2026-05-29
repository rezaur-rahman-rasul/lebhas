import { Routes } from '@angular/router';

import { promptBuilderGuard } from '@app/core/guards/prompt-access.guard';
import { roleGuard } from '@app/core/guards/role.guard';
import { ACTIVITY_ROUTES } from '../activity/activity.routes';
import { ASSET_ROUTES } from '../admin/assets/assets.routes';
import { CREATIVE_GENERATION_ROUTES } from '../admin/creative-generation/creative-generation.routes';
import { AI_MONITORING_ROUTES } from '../ai-monitoring/ai-monitoring.routes';
import { AUDIT_ROUTES } from '../audit/audit.routes';
import { MONITORING_ROUTES } from '../monitoring/monitoring.routes';
import { PAYMENT_ROUTES } from '../payments/payment.routes';
import { NOTIFICATION_ROUTES } from '../notifications/notification.routes';
import { PROFILE_ROUTES } from '../profile/profile.routes';
import { USAGE_BILLING_ROUTES } from '../usage-billing/usage-billing.routes';

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
    path: 'master/dashboard',
    canActivate: [roleGuard(['MASTER'])],
    loadComponent: () =>
      import('../master/dashboard/master-dashboard').then((m) => m.MasterDashboardPage),
  },
  {
    path: 'assets',
    children: ASSET_ROUTES,
  },
  {
    path: 'creative-generator',
    children: CREATIVE_GENERATION_ROUTES,
  },
  {
    path: 'generated-versions',
    loadComponent: () =>
      import('../admin/creative-generation/pages/generation-history/generation-history').then(
        (m) => m.GenerationHistoryPage,
      ),
  },
  {
    path: 'post-generator',
    loadComponent: () =>
      import('../admin/text-tools/pages/text-tool-page/text-tool-page').then((m) => m.TextToolPage),
    data: { tool: 'post' },
  },
  {
    path: 'captions',
    loadComponent: () =>
      import('../admin/text-tools/pages/text-tool-page/text-tool-page').then((m) => m.TextToolPage),
    data: { tool: 'caption' },
  },
  {
    path: 'ad-copy',
    loadComponent: () =>
      import('../admin/text-tools/pages/text-tool-page/text-tool-page').then((m) => m.TextToolPage),
    data: { tool: 'ads-copy' },
  },
  {
    path: 'hashtags',
    loadComponent: () =>
      import('../admin/text-tools/pages/text-tool-page/text-tool-page').then((m) => m.TextToolPage),
    data: { tool: 'hashtags' },
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
  {
    path: 'projects/:projectId/creative-requests',
    loadComponent: () =>
      import('../creative-requests/pages/project-creative-requests/project-creative-requests').then(
        (m) => m.ProjectCreativeRequestsPage,
      ),
  },
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
  {
    path: 'generated-versions/:generatedVersionId',
    loadComponent: () =>
      import('../generated-versions/pages/generated-version-detail/generated-version-detail').then(
        (m) => m.GeneratedVersionDetailPage,
      ),
  },
  {
    path: 'approvals',
    loadComponent: () =>
      import('../approvals/pages/approval-queue/approval-queue').then((m) => m.ApprovalQueuePage),
  },
  ...AI_MONITORING_ROUTES,
  ...USAGE_BILLING_ROUTES,
  ...PAYMENT_ROUTES,
  ...NOTIFICATION_ROUTES,
  ...PROFILE_ROUTES,
  ...ACTIVITY_ROUTES,
  ...AUDIT_ROUTES,
  ...MONITORING_ROUTES,
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'dashboard',
  },
];
