import { Routes } from '@angular/router';

import { RoleSectionPageComponent } from '@app/features/shared/role-section-page/role-section-page';

export const CREW_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./crew-home').then((m) => m.CrewHomeComponent),
  },
  {
    path: 'assets',
    loadChildren: () => import('../admin/assets/assets.routes').then((m) => m.ASSET_ROUTES),
  },
  {
    path: 'prompts',
    loadComponent: () =>
      import('../admin/prompts/pages/prompt-builder/prompt-builder').then(
        (m) => m.PromptBuilderPage,
      ),
  },
  {
    path: 'creative-generation',
    loadChildren: () =>
      import('../admin/creative-generation/creative-generation.routes').then(
        (m) => m.CREATIVE_GENERATION_ROUTES,
      ),
  },
  {
    path: 'generate-creative',
    component: RoleSectionPageComponent,
    data: {
      section: {
        eyebrow: 'Team',
        title: 'Generate Creative',
        description: 'Creative generation remains reserved for future implementation while crew access is enforced.',
        badgeLabel: 'CREW',
        badgeTone: 'blue',
        emptyIcon: 'sparkles',
        emptyTitle: 'Creative generation is pending',
        emptyDescription: 'Day 2 provides the auth shell, not the creative workflow itself.',
        highlights: [
          { title: 'Crew scope', description: 'Crew routes remain isolated from admin-only navigation.' },
          { title: 'Workspace lock', description: 'Workspace guard keeps crew members within their tenant.' },
          { title: 'Session refresh', description: 'Long-running authenticated sessions are already prepared.' },
        ],
      },
    },
  },
  {
    path: 'submissions',
    component: RoleSectionPageComponent,
    data: {
      section: {
        eyebrow: 'Team',
        title: 'Submissions',
        description: 'Submission tracking will sit here later without changing crew route protection.',
        badgeLabel: 'CREW',
        badgeTone: 'blue',
        emptyIcon: 'cloud-upload',
        emptyTitle: 'Submission workflow placeholder',
        emptyDescription: 'Review and submission features stay outside the Day 2 scope.',
        highlights: [
          { title: 'Access continuity', description: 'Crew navigation is already role-filtered in the layout.' },
          { title: 'Tenant-aware shell', description: 'Submission APIs can inherit the same workspace headers.' },
          { title: 'Operational traceability', description: 'Correlation IDs are attached for later workflow APIs.' },
        ],
      },
    },
  },
];
