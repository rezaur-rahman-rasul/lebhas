import { Routes } from '@angular/router';

export const ASSET_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/asset-library/asset-library').then((m) => m.AssetLibraryPage),
  },
  {
    path: 'projects/:projectId',
    loadComponent: () =>
      import('./pages/project-assets/project-assets').then((m) => m.ProjectAssetsPage),
  },
  {
    path: ':assetId',
    loadComponent: () => import('./pages/asset-detail/asset-detail').then((m) => m.AssetDetailPage),
  },
];
