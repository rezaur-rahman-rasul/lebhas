import { ChangeDetectionStrategy, Component, effect, inject } from '@angular/core';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { BillingModalService } from '@app/features/payments/services/billing-modal.service';
import { BuyCreditsCardComponent } from '../../components/buy-credits-card/buy-credits-card';
import { CreditBalanceCardComponent } from '../../components/credit-balance-card/credit-balance-card';
import { CreditLedgerTableComponent } from '../../components/credit-ledger-table/credit-ledger-table';
import { FreeCreditStatusCardComponent } from '../../components/free-credit-status-card/free-credit-status-card';
import { WorkspaceCreditsStore } from '../../state/workspaceCredits.store';

@Component({ selector: 'app-workspace-credits-page', standalone: true, imports: [ButtonComponent, AppErrorStateComponent, PageHeaderComponent, BuyCreditsCardComponent, CreditBalanceCardComponent, CreditLedgerTableComponent, FreeCreditStatusCardComponent], templateUrl: './workspace-credits-page.html', styleUrl: './workspace-credits-page.scss', changeDetection: ChangeDetectionStrategy.OnPush })
export class WorkspaceCreditsPage {
  protected readonly workspace = inject(WorkspaceStore);
  protected readonly credits = inject(WorkspaceCreditsStore);
  private readonly billing = inject(BillingModalService);
  constructor() { effect(() => { const id = this.workspace.activeWorkspaceId(); if (id) { void this.credits.loadAccount(id); void this.credits.loadLedger(id); } }); }
  protected refresh(): void { const id = this.workspace.activeWorkspaceId(); if (id) { void this.credits.loadAccount(id); void this.credits.loadLedger(id); } }
  protected buyCredits(): void { this.billing.show(); }
}
