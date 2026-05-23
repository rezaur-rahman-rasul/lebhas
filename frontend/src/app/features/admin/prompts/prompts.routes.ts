import { Routes } from '@angular/router';

export const PROMPT_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/prompt-builder/prompt-builder').then((m) => m.PromptBuilderPage),
  },
  {
    path: 'templates',
    loadComponent: () =>
      import('./pages/prompt-templates/prompt-templates').then((m) => m.PromptTemplatesPage),
  },
  {
    path: 'history',
    loadComponent: () =>
      import('./pages/prompt-history/prompt-history').then((m) => m.PromptHistoryPage),
  },
];
