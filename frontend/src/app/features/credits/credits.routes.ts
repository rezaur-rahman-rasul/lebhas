import { Routes } from '@angular/router';
import { creditLedgerAccessGuard } from '@app/core/guards/usage-billing-access.guard';

export const CREDIT_ROUTES: Routes = [
  {
    path: 'credits',
    loadComponent: () => import('./pages/workspace-credits-page/workspace-credits-page').then((m) => m.WorkspaceCreditsPage),
  },
  {
    path: 'credits/ledger',
    canActivate: [creditLedgerAccessGuard],
    loadComponent: () => import('./pages/credit-ledger-page/credit-ledger-page').then((m) => m.WorkspaceCreditLedgerPage),
  },
];
