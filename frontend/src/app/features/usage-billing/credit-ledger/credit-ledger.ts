import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { EmptyStateComponent } from '@app/shared/components/app-empty-state/app-empty-state';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';
import { CreditLedgerCardComponent } from '../components/credit-ledger-card/credit-ledger-card';
import { CreditLifecycleTimelineComponent } from '../components/credit-lifecycle-timeline/credit-lifecycle-timeline';
import { UsageEmptyStateComponent } from '../components/usage-empty-state/usage-empty-state';
import { UsageFilterBarComponent } from '../components/usage-filter-bar/usage-filter-bar';
import { UsageLoadingStateComponent } from '../components/usage-loading-state/usage-loading-state';
import { CreditLedger, CreditTransactionType, UsageType } from '../models/usage-billing.models';
import { UsageBillingStore } from '../state/usage-billing.store';

@Component({
  selector: 'app-credit-ledger-page',
  standalone: true,
  imports: [
    DatePipe,
    DecimalPipe,
    RouterLink,
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    BadgeComponent,
    CreditLedgerCardComponent,
    CreditLifecycleTimelineComponent,
    UsageEmptyStateComponent,
    UsageFilterBarComponent,
    UsageLoadingStateComponent,
  ],
  templateUrl: './credit-ledger.html',
  styleUrl: './credit-ledger.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreditLedgerPage {
  protected readonly permissions = inject(PermissionStore);
  protected readonly workspace = inject(WorkspaceStore);
  protected readonly store = inject(UsageBillingStore);

  protected readonly activeWorkspaceId = this.workspace.activeWorkspaceId;
  protected readonly accessDenied = computed(() => !this.permissions.canViewCreditLedger());
  protected readonly transactions = this.store.creditLedger;
  protected readonly hasTransactions = computed(() => this.transactions().length > 0);

  constructor() {
    effect(() => {
      const workspaceId = this.activeWorkspaceId();
      if (!workspaceId || !this.permissions.canViewCreditLedger()) {
        return;
      }

      void this.store.loadCreditLedger(workspaceId);
    });
  }

  protected refresh(): void {
    const workspaceId = this.activeWorkspaceId();
    if (!workspaceId || !this.permissions.canViewCreditLedger()) {
      return;
    }

    void this.store.loadCreditLedger(workspaceId);
  }

  protected updateMonth(month: string | null): void {
    this.store.setSelectedMonth(month);
    this.refresh();
  }

  protected updateUsageType(type: UsageType | string | null): void {
    this.store.setSelectedUsageType(type);
  }

  protected updateTransactionType(type: CreditTransactionType | string | null): void {
    this.store.setSelectedTransactionType(type);
    this.refresh();
  }

  protected clearFilters(): void {
    this.store.setSelectedMonth(null);
    this.store.setSelectedTransactionType(null);
    this.refresh();
  }

  protected transactionLabel(value: string | null): string {
    return toTitleCase(value || 'Credit update');
  }

  protected transactionTone(entry: CreditLedger): 'neutral' | 'brand' | 'blue' | 'red' {
    switch (entry.transactionType) {
      case CreditTransactionType.Refund:
        return 'brand';
      case CreditTransactionType.Reserve:
        return 'blue';
      case CreditTransactionType.Expiry:
        return 'red';
      default:
        return 'neutral';
    }
  }

  protected amountPrefix(entry: CreditLedger): string {
    return entry.transactionType === CreditTransactionType.Refund ? '+' : '';
  }

  protected explanation(entry: CreditLedger): string {
    switch (entry.transactionType) {
      case CreditTransactionType.Reserve:
        return 'Credits are temporarily held for generation.';
      case CreditTransactionType.Finalize:
        return 'Credits were used after successful generation.';
      case CreditTransactionType.Refund:
        return 'Credits returned after failed generation.';
      default:
        return entry.description || 'Credit balance was updated.';
    }
  }
}

function toTitleCase(value: string): string {
  return value
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}
