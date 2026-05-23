import { Routes } from '@angular/router';

export const CREATIVE_GENERATION_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/creative-generator/creative-generator').then((m) => m.CreativeGeneratorPage),
  },
  {
    path: 'history',
    loadComponent: () =>
      import('./pages/generation-history/generation-history').then((m) => m.GenerationHistoryPage),
  },
  {
    path: 'outputs/:outputId',
    loadComponent: () =>
      import('./pages/creative-output-detail/creative-output-detail').then(
        (m) => m.CreativeOutputDetailPage,
      ),
  },
];
