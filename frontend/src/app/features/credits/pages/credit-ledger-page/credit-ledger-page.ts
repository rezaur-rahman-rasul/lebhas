import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { CreditLedgerTableComponent } from '../../components/credit-ledger-table/credit-ledger-table';
import { WorkspaceCreditsStore } from '../../state/workspaceCredits.store';

@Component({ selector: 'app-workspace-credit-ledger-page', standalone: true, imports: [ReactiveFormsModule, ButtonComponent, AppErrorStateComponent, PageHeaderComponent, CreditLedgerTableComponent], templateUrl: './credit-ledger-page.html', styleUrl: './credit-ledger-page.scss', changeDetection: ChangeDetectionStrategy.OnPush })
export class WorkspaceCreditLedgerPage {
  protected readonly workspace = inject(WorkspaceStore);
  protected readonly credits = inject(WorkspaceCreditsStore);
  protected readonly filtersOpen = signal(true);
  protected readonly form = new FormGroup({ transactionType: new FormControl(''), from: new FormControl(''), to: new FormControl(''), search: new FormControl('') });
  constructor() { effect(() => { const id = this.workspace.activeWorkspaceId(); if (id) void this.credits.loadLedger(id); }); }
  protected apply(): void { const id = this.workspace.activeWorkspaceId(); if (id) void this.credits.loadLedger(id, this.form.getRawValue()); }
  protected clear(): void { this.form.reset({ transactionType: '', from: '', to: '', search: '' }); this.apply(); }
}
